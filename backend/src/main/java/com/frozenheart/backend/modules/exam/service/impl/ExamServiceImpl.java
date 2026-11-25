package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.IdCursorPage;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.*;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.DifficultyDto;
import com.frozenheart.backend.modules.exam.dto.ExamBankSummary;
import com.frozenheart.backend.modules.exam.dto.ExamDetailResponse;
import com.frozenheart.backend.modules.exam.dto.ExamListResponse;
import com.frozenheart.backend.modules.exam.dto.ExamMatrix;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.GenerateExamRequest;
import com.frozenheart.backend.modules.exam.dto.Statistic;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.exam.repository.ExamCourseClassRepository;
import com.frozenheart.backend.modules.exam.repository.ExamQuestionRepository;
import com.frozenheart.backend.modules.exam.repository.ExamRepository;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
import com.frozenheart.backend.modules.exam.service.ExamService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.repository.TopicRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final DocumentExportService documentExportService;
    private final JsonMapper jsonMapper;
    private final ExamCourseClassRepository examCourseClassRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final CourseClassRepository courseClassRepository;
    private final TopicRepository topicRepository;

    @Override
    @Transactional(readOnly = true)
    public ExamBankSummary getBankSummary(Long subjectId) {
        JwtPayload actor = JwtPayload.getCurrentUserPayload();
        UserRole role = UserRole.valueOf(actor.getRole());
        if (role != UserRole.ADMIN && (role != UserRole.LECTURER
                || topicRepository.countTeachingAssignments(subjectId, actor.getUserId()) == 0)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không phụ trách môn học này");
        }
        if (!subjectRepository.existsById(subjectId)) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy môn học");
        }

        var total = new EnumMap<QuestionDifficulty, Integer>(QuestionDifficulty.class);
        Map<Long, Map<QuestionDifficulty, Integer>> perTopic = new HashMap<>();
        for (var count : questionRepository.countExamBankByTopicAndDifficulty(subjectId)) {
            var difficulty = Objects.requireNonNullElse(count.getDifficulty(), QuestionDifficulty.UNCLASSIFIED);
            int size = Math.toIntExact(count.getQuestionCount());
            total.merge(difficulty, size, Integer::sum);
            if (count.getTopicId() != null) {
                perTopic.computeIfAbsent(count.getTopicId(), ignored -> new EnumMap<>(QuestionDifficulty.class))
                        .merge(difficulty, size, Integer::sum);
            }
        }
        var topics = topicRepository.findBySubjectIdOrderByNameAsc(subjectId).stream().filter(Topic::isActive)
                .map(topic -> {
                    var counts = perTopic.getOrDefault(topic.getId(), Map.of());
                    return new ExamBankSummary.TopicStock(topic.getId(), topic.getName(), totalCount(counts),
                            ExamQuestionSelection.statistic(counts));
                }).toList();
        return new ExamBankSummary(subjectId, totalCount(total), ExamQuestionSelection.statistic(total), topics);
    }

    private static int totalCount(Map<QuestionDifficulty, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    @Transactional
    public ExamDetailResponse generateExam(GenerateExamRequest request) {

        if (request.getClassIds() == null || request.getClassIds().isEmpty()) {
            throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "Phải có lớp học mới gán được đề thi");
        }

        JwtPayload jwtPayload = JwtPayload.getCurrentUserPayload();
        Long lecturerId = jwtPayload.getUserId();
        UserRole role = UserRole.valueOf(jwtPayload.getRole());

        if (UserRole.STUDENT.equals(role)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Sinh viên không có quyền tạo đề thi");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy môn học"));

        User lecturer = userRepository.getReferenceById(lecturerId);

        int targetTotal = request.getQuestionCount() != null ? request.getQuestionCount() : 10;
        var weights = request.getTopicWeights();
        var catalog = weights == null || weights.isEmpty() ? List.<Topic>of()
                : topicRepository.findBySubjectIdOrderByNameAsc(subject.getId());
        var selection = ExamQuestionSelection.select(questionRepository.findApprovedBySubjectId(subject.getId()),
                targetTotal, request.getDifficultyDistribution(), weights, catalog);
        var selectedQuestions = selection.questions();

        String examTitle = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : "Đề thi " + subject.getName() + " - " + System.currentTimeMillis() / 1000;

        Instant now = Instant.now();

        Exam exam = Exam.builder()
                .title(examTitle)
                .subject(subject)
                .lecturer(lecturer)
                .questionCount(selectedQuestions.size())
                .difficultyDistributionJson(jsonMapper.writeValueAsString(selection.difficulty()))
                .topicWeightsJson(jsonMapper.writeValueAsString(selection.topics()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Exam savedExam = examRepository.save(exam);

        List<CourseClass> courseClasses = courseClassRepository.findAllByIds(request.getClassIds());

        Set<ExamCourseClass> examCourseClasses = new HashSet<>();
        for (CourseClass courseClass : courseClasses) {

            ExamCourseClassId id = ExamCourseClassId.builder()
                    .examId(savedExam.getId())
                    .courseClassId(courseClass.getId())
                    .build();

            examCourseClasses.add(ExamCourseClass.builder()
                    .id(id)
                    .exam(savedExam)
                    .courseClass(courseClass)
                    .examCode(subject.getId() + "_" + courseClass.getId() + "_" + savedExam.getTitle() + "_"
                            + savedExam.getId())
                    .build());
        }

        if (!examCourseClasses.isEmpty()) {
            examCourseClassRepository.saveAll(examCourseClasses);
        }

        // Create ExamQuestion Snapshots
        Set<ExamQuestion> examQuestions = new HashSet<>();
        int order = 1;
        boolean shuffle = Boolean.TRUE.equals(request.getShuffleOptions());

        for (Question q : selectedQuestions) {
            List<QuestionOption> optionsSnapshot = prepareOptionsSnapshot(q.getOptions(), shuffle);

            ExamQuestionId eqId = new ExamQuestionId(savedExam.getId(), q.getId());
            ExamQuestion eq = ExamQuestion.builder()
                    .id(eqId)
                    .exam(savedExam)
                    .question(q)
                    .displayOrder(order++)
                    .optionsSnapshot(optionsSnapshot)
                    .build();

            examQuestions.add(eq);
        }

        savedExam.setExamQuestions(examQuestions);

        examQuestionRepository.saveAll(examQuestions);
        examRepository.save(savedExam);

        return mapToDetailResponse(savedExam);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDetailResponse getExamDetail(Long examId) {
        return mapToDetailResponse(loadExamForManagement(examId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamListResponse getMyExams(Long subjectId, Long after, Long before, int limit) {
        Long lecturerId = JwtPayload.getCurrentUserPayload().getUserId();

        int safePageSize = Math.clamp(limit, 1, 20);
        var page = IdCursorPage.load(after, before, safePageSize, Exam::getId,
                (next, previous, request) -> examRepository.findMyExamsCursor(lecturerId, subjectId, next, previous, request));
        List<Exam> pageContent = page.items();

        List<Long> examIds = pageContent.stream().map(Exam::getId).distinct().toList();
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamsIdWithQuestionAndTopic(examIds);

        Map<Long, List<ExamQuestion>> examIdToQuestionsMap = examQuestions.stream().collect(
                Collectors.groupingBy(eq -> eq.getExam().getId()));

        List<ExamListResponse.ExamItem> items = pageContent.stream().map(e -> {

            List<ExamQuestion> questions = examIdToQuestionsMap.getOrDefault(e.getId(), List.of());

            int easy = 0, medium = 0, hard = 0, unclassified = 0;

            for (ExamQuestion eq : questions) {
                QuestionDifficulty d = eq.getQuestion().getDifficulty();
                if (d == QuestionDifficulty.EASY)
                    easy++;
                else if (d == QuestionDifficulty.MEDIUM)
                    medium++;
                else if (d == QuestionDifficulty.HARD)
                    hard++;
                else
                    unclassified++;
            }

            return ExamListResponse.ExamItem.builder()
                    .examId(e.getId())
                    .title(e.getTitle())
                    .subjectName(e.getSubject() != null ? e.getSubject().getName() : "")
                    .questionCount(e.getQuestionCount())
                    .createdAt(e.getCreatedAt())
                    .statistic(Statistic.builder()
                            .easyCount(easy)
                            .mediumCount(medium)
                            .hardCount(hard)
                            .unclassifiedCount(unclassified)
                            .build())
                    .downloadUrl(e.getPdfFileUrl())
                    .build();
        }).collect(Collectors.toList());

        return ExamListResponse.builder()
                .items(items)
                .pagination(page.pagination())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResponse exportExam(Long examId, String format, Boolean includeAnswerKey) {
        Exam exam = loadExamForManagement(examId);
        return documentExportService.exportExam(exam, format, includeAnswerKey);
    }

    private Exam loadExamForManagement(Long examId) {
        JwtPayload user = JwtPayload.getCurrentUserPayload();
        boolean admin = UserRole.ADMIN.name().equals(user.getRole());
        if (!admin && !UserRole.LECTURER.name().equals(user.getRole())) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền truy cập đề thi này");
        }

        Exam exam = examRepository.findWithDetailsById(examId)
                .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy đề thi"));
        if (!admin && (exam.getLecturer() == null || !exam.getLecturer().getId().equals(user.getUserId()))) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền truy cập đề thi này");
        }
        return exam;
    }

    // --- Helpers ---

    private String generateOptionKey(int index) {

        StringBuilder key = new StringBuilder();

        index++;

        while (index > 0) {
            index--;

            key.append((char) ('A' + index % 26));
            index /= 26;
        }

        return key.reverse().toString();

    }

    private List<QuestionOption> prepareOptionsSnapshot(List<QuestionOption> originalOptions, boolean shuffle) {
        if (originalOptions == null || originalOptions.isEmpty())
            return Collections.emptyList();
        List<QuestionOption> list = new ArrayList<>(originalOptions);
        if (shuffle) {
            Collections.shuffle(list);

            int size = list.size();
            for (int i = 0; i < size; i++) {
                QuestionOption opt = list.get(i);
                QuestionOption copy = new QuestionOption();
                copy.setKey(generateOptionKey(i));
                copy.setText(opt.getText());
                copy.setIsCorrect(opt.getIsCorrect());
                copy.setMediaUrl(opt.getMediaUrl());
                copy.setMediaId(opt.getMediaId());
                list.set(i, copy);
            }
        }
        return list;
    }

    private ExamDetailResponse mapToDetailResponse(Exam exam) {
        int easy = 0, medium = 0, hard = 0, unclassified = 0;
        List<ExamDetailResponse.ExamQuestionDto> qDtos = new ArrayList<>();

        if (exam.getExamQuestions() != null) {
            for (ExamQuestion eq : exam.getExamQuestions()) {
                Question q = eq.getQuestion();
                QuestionDifficulty d = q.getDifficulty();
                if (d == QuestionDifficulty.EASY)
                    easy++;
                else if (d == QuestionDifficulty.MEDIUM)
                    medium++;
                else if (d == QuestionDifficulty.HARD)
                    hard++;
                else
                    unclassified++;

                List<String> imageUrls = q.getOwnedMedias() != null
                        ? q.getOwnedMedias().stream()
                                .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                                .map(QuestionMedia::getUrl)
                                .collect(Collectors.toList())
                        : Collections.emptyList();

                List<ExamDetailResponse.OptionDto> optionDtos = new ArrayList<>();

                if (eq.getOptionsSnapshot() != null) {
                    for (QuestionOption opt : eq.getOptionsSnapshot()) {
                        optionDtos.add(ExamDetailResponse.OptionDto.builder()
                                .key(opt.getKey())
                                .text(opt.getText())
                                .mediaUrl(opt.getMediaUrl())
                                .isCorrect(opt.getIsCorrect())
                                .build());
                    }
                }

                DifficultyDto difficulty = DifficultyDto
                        .valueOf(q.getDifficulty() != null ? q.getDifficulty().name()
                                : QuestionDifficulty.UNCLASSIFIED.name());

                qDtos.add(ExamDetailResponse.ExamQuestionDto.builder()
                        .order(eq.getDisplayOrder())
                        .questionId(q.getId())
                        .content(q.getContent())
                        .imageUrls(imageUrls)
                        .options(optionDtos)
                        .difficulty(difficulty)
                        .topic(q.getTopic() != null ? q.getTopic().getName() : "")
                        .build());
            }
        }

        var difficultyMatrix = readMatrix(exam.getDifficultyDistributionJson(), "actual", ExamMatrix.Difficulty.class);
        var topicMatrix = readMatrix(exam.getTopicWeightsJson(), "rows", ExamMatrix.Topics.class);
        return ExamDetailResponse.builder()
                .examId(exam.getId())
                .title(exam.getTitle())
                .subjectName(exam.getSubject() != null ? exam.getSubject().getName() : "")
                .questionCount(exam.getQuestionCount())
                .questions(qDtos)
                .difficultyMatrix(difficultyMatrix)
                .topicMatrix(topicMatrix)
                .statistic(difficultyMatrix != null ? difficultyMatrix.actual() : Statistic.builder()
                        .easyCount(easy)
                        .mediumCount(medium)
                        .hardCount(hard)
                        .unclassifiedCount(unclassified)
                        .build())
                .createdAt(exam.getCreatedAt())
                .build();
    }

    private <T> T readMatrix(String json, String snapshotField, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try {
            var node = jsonMapper.readTree(json);
            return node != null && node.hasNonNull(snapshotField) ? jsonMapper.treeToValue(node, type) : null;
        } catch (JacksonException e) {
            log.warn("Could not read saved exam matrix of type {}", type.getSimpleName(), e);
            return null;
        }
    }
}
