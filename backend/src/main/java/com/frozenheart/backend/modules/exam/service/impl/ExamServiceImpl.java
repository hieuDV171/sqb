package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.*;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.DifficultyDto;
import com.frozenheart.backend.modules.exam.dto.ExamDetailResponse;
import com.frozenheart.backend.modules.exam.dto.ExamListResponse;
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
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        // Fetch APPROVED questions
        List<Question> approvedQuestions = questionRepository.findApprovedBySubjectId(request.getSubjectId());
        if (approvedQuestions.isEmpty()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    "Ngân hàng câu hỏi đã duyệt (APPROVED) cho môn học này đang trống");
        }

        // Filter & Group by difficulty
        Map<QuestionDifficulty, List<Question>> byDifficulty = approvedQuestions.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getDifficulty() != null ? q.getDifficulty() : QuestionDifficulty.UNCLASSIFIED));

        int targetTotal = request.getQuestionCount() != null ? request.getQuestionCount() : 10;
        GenerateExamRequest.DifficultyDistribution dist = request.getDifficultyDistribution();

        int targetEasy = dist != null && dist.getEasy() != null ? (int) Math.round(targetTotal * dist.getEasy())
                : (int) Math.round(targetTotal * 0.0);
        int targetMedium = dist != null && dist.getMedium() != null ? (int) Math.round(targetTotal * dist.getMedium())
                : (int) Math.round(targetTotal * 0.0);
        int targetHard = dist != null && dist.getHard() != null ? (int) Math.round(targetTotal * dist.getHard())
                : (int) Math.round(targetTotal * 0.0);
        int targetUnclassified = dist != null && dist.getUnclassified() != null
                ? (int) Math.round(targetTotal * dist.getUnclassified())
                : (dist == null ? targetTotal : 0);

        List<Question> selectedQuestions = new ArrayList<>();
        selectedQuestions.addAll(
                pickQuestions(byDifficulty.getOrDefault(QuestionDifficulty.EASY, Collections.emptyList()), targetEasy));
        selectedQuestions.addAll(pickQuestions(
                byDifficulty.getOrDefault(QuestionDifficulty.MEDIUM, Collections.emptyList()), targetMedium));
        selectedQuestions.addAll(
                pickQuestions(byDifficulty.getOrDefault(QuestionDifficulty.HARD, Collections.emptyList()), targetHard));
        selectedQuestions.addAll(
                pickQuestions(byDifficulty.getOrDefault(QuestionDifficulty.UNCLASSIFIED, Collections.emptyList()),
                        targetUnclassified));

        // Nếu thừa câu do làm tròn -> Cắt về đúng targetTotal
        if (selectedQuestions.size() > targetTotal) {
            selectedQuestions = new ArrayList<>(selectedQuestions.subList(0, targetTotal));
        }
        // Nếu thiếu câu -> Backfill từ danh sách còn lại
        else if (selectedQuestions.size() < targetTotal) {
            Set<Long> selectedIds = selectedQuestions.stream().map(Question::getId).collect(Collectors.toSet());
            List<Question> remaining = approvedQuestions.stream()
                    .filter(q -> !selectedIds.contains(q.getId()))
                    .collect(Collectors.toList());

            selectedQuestions.addAll(pickQuestions(remaining, targetTotal - selectedQuestions.size()));
        }

        Collections.shuffle(selectedQuestions);

        // Serialize JSON metadata
        String diffJson = "";
        String topicJson = "";
        try {
            diffJson = jsonMapper.writeValueAsString(request.getDifficultyDistribution());
            topicJson = jsonMapper.writeValueAsString(request.getTopicWeights());
        } catch (Exception e) {
            log.warn("Failed to serialize difficulty/topic weights", e);
        }

        String examTitle = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : "Đề thi " + subject.getName() + " - " + System.currentTimeMillis() / 1000;

        Instant now = Instant.now();

        Exam exam = Exam.builder()
                .title(examTitle)
                .subject(subject)
                .lecturer(lecturer)
                .questionCount(selectedQuestions.size())
                .difficultyDistributionJson(diffJson)
                .topicWeightsJson(topicJson)
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
        Exam exam = examRepository.findWithDetailsById(examId)
                .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy đề thi"));
        return mapToDetailResponse(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamListResponse getMyExams(Long subjectId, Long after, int limit) {
        Long lecturerId = JwtPayload.getCurrentUserPayload().getUserId();

        int safePageSize = Math.clamp(limit, 1, 20);
        List<Exam> exams = examRepository.findMyExamsCursor(lecturerId, subjectId, after,
                PageRequest.of(0, safePageSize + 1));

        boolean hasNext = exams.size() > safePageSize;
        List<Exam> pageContent = hasNext ? exams.subList(0, safePageSize) : exams;

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

        Long nextAfter = pageContent.isEmpty() ? null : pageContent.getLast().getId();

        return ExamListResponse.builder()
                .items(items)
                .pagination(CursorPaginationDto.builder()
                        .after(nextAfter)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResponse exportExam(Long examId, String format, Boolean includeAnswerKey, String paperSize) {
        Exam exam = examRepository.findWithDetailsById(examId)
                .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy đề thi"));
        return documentExportService.exportExam(exam, format, includeAnswerKey, paperSize);
    }

    // --- Helpers ---

    private List<Question> pickQuestions(List<Question> source, int count) {
        if (source.isEmpty() || count <= 0)
            return Collections.emptyList();
        List<Question> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

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

        return ExamDetailResponse.builder()
                .examId(exam.getId())
                .title(exam.getTitle())
                .subjectName(exam.getSubject() != null ? exam.getSubject().getName() : "")
                .questionCount(exam.getQuestionCount())
                .questions(qDtos)
                .statistic(Statistic.builder()
                        .easyCount(easy)
                        .mediumCount(medium)
                        .hardCount(hard)
                        .unclassifiedCount(unclassified)
                        .build())
                .createdAt(exam.getCreatedAt())
                .build();
    }
}
