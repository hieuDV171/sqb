package com.frozenheart.backend.modules.session.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.ProcessingStatus;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.session.dto.MySubmissionDetailResponse;
import com.frozenheart.backend.modules.session.dto.CourseClassOptionResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest.QuestionProposeDto;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.dto.UpdateSubmissionSessionRequest;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.core.dto.event.MediaCleanupEvent;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;
import com.frozenheart.backend.modules.session.service.SessionService;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.repository.UserCourseClassRepository;
import com.frozenheart.backend.core.entity.user.UserCourseClass;
import com.frozenheart.backend.modules.session.dto.SubmissionProjection;
import com.frozenheart.backend.modules.session.dto.SubmissionsResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import com.frozenheart.backend.modules.user.service.CounterMetricsService;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final AnonymizerUtil anonymizerUtil;
    private final UserRepository userRepository;
    private final CourseClassRepository courseClassRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final CounterMetricsService counterMetricsService;
    private final MediaService mediaService;
    private final ApplicationEventPublisher eventPublisher;
    private final MetricService metricService;
    private final UserCourseClassRepository userCourseClassRepository;
    private final UserProfileRepository userProfileRepository;
    private final CurrentSemesterHolder currentSemesterHolder;

    @Transactional
    @Override
    public ProposeSessionResponse proposeSession(ProposeSessionRequest request) {
        if (request == null) {
            throw new AppException(
                    ResponseCode.MISSING_REQUIRED_PARAMETER,
                    "Dữ liệu đề xuất phiên không được để trống");
        }

        List<QuestionProposeDto> requestedQuestions = request.questions();
        if (requestedQuestions == null || requestedQuestions.isEmpty()) {
            throw new AppException(
                    ResponseCode.MISSING_REQUIRED_PARAMETER,
                    "Danh sách câu hỏi không được để trống");
        }

        // Hiện tại, nội dung câu hỏi có thể có nhiều ảnh, nhưng đáp án chỉ được 1 ảnh

        // Lấy người nộp và xác thực chính xác lớp học phần thuộc học kỳ hiện tại.
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        User proposer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        CourseClass courseClass = resolveAuthorizedActiveCourseClass(
                userId, proposer.getRole(), request.courseClassId());
        Subject subject = courseClass.getSubject();

        // Mã ẩn danh 6 ký tự của tác giả
        String authorCode = anonymizerUtil.encodeUserId(userId);

        // Fail-Fast: Thu thập toàn bộ URL ảnh từ request và kiểm tra tính toàn vẹn TRƯỚC KHI tạo Session / Question
        List<String> allRequestedMediaUrls = new ArrayList<>();
        for (var qDto : requestedQuestions) {
            if (qDto.mediaUrls() != null) {
                allRequestedMediaUrls.addAll(qDto.mediaUrls());
            }
            if (qDto.options() != null) {
                for (var opt : qDto.options()) {
                    if (opt != null && opt.getMediaUrl() != null && !opt.getMediaUrl().isBlank()) {
                        allRequestedMediaUrls.add(opt.getMediaUrl());
                    }
                }
            }
        }

        if (!allRequestedMediaUrls.isEmpty()) {
            List<String> missingMedias = mediaService.findMissingObjects(allRequestedMediaUrls);
            if (!missingMedias.isEmpty()) {
                throw new AppException(ResponseCode.RESOURCE_NOT_FOUND,
                        "Các tệp ảnh sau đã hết hạn lưu tạm thời trên hệ thống lưu trữ hoặc không tồn tại: " + missingMedias + ". Vui lòng tải lại ảnh trước khi đề xuất phiên.");
            }
        }

        // Khởi tạo & Lưu Session
        Session session = Session.builder()
                .title(request.title())
                .content(request.content())
                .sourceUrl(request.sourceUrl())
                .proposer(proposer)
                .courseClass(courseClass)
                .status(SessionStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        // phiên chưa public, không đánh index (có gọi đánh index thì ES cũng không đánh
        // index)
        session = sessionRepository.save(session);

        // {subjectCode}_{authorCode}_{timestamp}_{sessionId}
        String sessionCode = subject.getCode() + "_" + authorCode + "_" + System.currentTimeMillis() + "_"
                + session.getId();
        session.setSessionCode(sessionCode);

        // Chuyển đổi danh sách DTO sang danh sách Question Entity
        Session finalSession = session;

        List<Question> questions = IntStream.range(0, requestedQuestions.size())
                .mapToObj(index -> {
                    QuestionProposeDto q = requestedQuestions.get(index);
                    String textContent = q.content();

                    Instant now = Instant.now();
                    return Question.builder()
                            .session(finalSession) // Gán mối quan hệ với Session

                            .content(textContent)

                            .difficulty(QuestionDifficulty.UNCLASSIFIED)
                            .status(QuestionStatus.PENDING)

                            .options(q.options())
                            .explanation(q.explanation())

                            .llmGenerated(q.llmGenerated())
                            .confidenceScore(q.confidence() != null ? q.confidence() : 0.0)
                            .displayOrder(index + 1)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                }).toList();

        // Lưu câu hỏi vào DB
        // Đang PENDING, không index
        List<Question> savedQuestions = questionRepository.saveAll(questions);

        // Cập nhật questionCode và Lưu QuestionMedia đính kèm
        List<QuestionMedia> mediaListToSave = new ArrayList<>();

        // Cập nhật questionCode cho từng câu hỏi
        int size = savedQuestions.size();
        for (int i = 0; i < size; i++) {
            Question q = savedQuestions.get(i);
            QuestionProposeDto dto = requestedQuestions.get(i);

            q.setQuestionCode(subject.getCode() + "_" + authorCode + "_" + q.getId());

            // Ảnh của Đề bài (CONTENT)
            if (dto.mediaUrls() != null && !dto.mediaUrls().isEmpty()) {
                for (String mediaUrl : dto.mediaUrls()) {
                    if (mediaUrl != null && !mediaUrl.isBlank()) {
                        QuestionMedia media = QuestionMedia.builder()
                                .url(mediaUrl)
                                .mediaTarget(MediaTarget.CONTENT)
                                .question(q)
                                .owner(proposer)
                                .processingStatus(ProcessingStatus.PENDING)
                                .uploadedAt(Instant.now())
                                .build();

                        mediaListToSave.add(media);
                    }
                }
            }

            // Ảnh của các Đáp án (OPTION)
            if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                for (var opt : q.getOptions()) {
                    if (opt != null && opt.getMediaUrl() != null && !opt.getMediaUrl().isBlank()) {
                        QuestionMedia media = QuestionMedia.builder()
                                .url(opt.getMediaUrl())
                                .mediaTarget(MediaTarget.OPTION)
                                .question(q)
                                .owner(proposer)
                                .processingStatus(ProcessingStatus.PENDING)
                                .uploadedAt(Instant.now())
                                .build();

                        mediaListToSave.add(media);
                    }
                }
            }
        }

        if (!mediaListToSave.isEmpty()) {
            List<QuestionMedia> savedMedias = questionMediaRepository.saveAll(mediaListToSave);
            List<String> mediaUrls = mediaListToSave.stream()
                    .map(QuestionMedia::getUrl)
                    .filter(Objects::nonNull)
                    .toList();
            if (!mediaUrls.isEmpty()) {
                mediaService.confirmMediaPermanent(mediaUrls);
            }

            // Tạo Map tra cứu siêu nhanh O(1) trên RAM: Key = questionId_mediaUrl -> Value
            // = mediaId
            Map<String, Long> mediaIdMap = savedMedias.stream()
                    .collect(Collectors.toMap(
                            m -> m.getQuestion().getId() + "_" + m.getUrl(), // key
                            QuestionMedia::getId, // value
                            (existing, _) -> existing // xử lý đụng độ
                    ));

            // Tra cứu O(1) để gán mediaId ngược lại cho từng QuestionOption
            for (Question q : savedQuestions) {
                if (q.getOptions() != null) {
                    for (var opt : q.getOptions()) {
                        if (opt != null && opt.getMediaUrl() != null) {
                            Long mediaId = mediaIdMap.get(q.getId() + "_" + opt.getMediaUrl());
                            if (mediaId != null) {
                                opt.setMediaId(mediaId);
                            }
                        }
                    }
                }
            }
        }

        // Cập nhật chỉ số totalProposedQuestion cho sinh viên đề xuất
        counterMetricsService.incrementProposedQuestions(proposer.getId(), requestedQuestions.size());

        metricService.incrementCounter("sqb.exam.sessions.created", "subject", subject.getCode());
        metricService.incrementCounter("sqb.exam.questions.proposed", requestedQuestions.size(), "subject", subject.getCode());

        // BẮN TASK CHẠY NGẦM CHECK TRÙNG 3 TẦNG
        duplicateDetectionService.asyncCheckDuplicates(session.getId());

        return ProposeSessionResponse.builder()
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .subjectId(subject.getId())
                .courseClassId(courseClass.getId())
                .semesterName(courseClass.getSemester().getName())
                .questionCount(requestedQuestions.size())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionsResponse getMySubmissions(
            Long after, Integer limit, Long subjectId, SessionStatus status) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        return getUserSubmissions(currentUserId, after, limit, subjectId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionsResponse getUserSubmissions(
            Long userId, Long after, Integer limit, Long subjectId, SessionStatus status) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        // Business Rule:
        // - Nếu xem chính mình (userId == currentUserId): cho phép xem theo status yêu cầu (hoặc tất cả nếu status null)
        // - Nếu xem người khác (userId != currentUserId): bắt buộc chỉ xem các phiên đã được duyệt (RESOLVED)
        boolean isOwner = currentUserId != null && currentUserId.equals(userId);
        SessionStatus effectiveStatus = isOwner ? status : SessionStatus.RESOLVED;

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<SubmissionProjection> mySubmissions = sessionRepository.findSubmissions(userId, subjectId, effectiveStatus, after, pageable);

        boolean hasNext = false;
        if (mySubmissions.size() > pageSize) {
            hasNext = true;
            mySubmissions = mySubmissions.subList(0, pageSize);
        }

        Long nextAfter = null;
        if (!mySubmissions.isEmpty()) {
            nextAfter = mySubmissions.getLast().getSessionId();
        }

        List<SubmissionsResponse.SubmissionSessionSummaryDto> contents = mySubmissions.stream()
                .map(s -> SubmissionsResponse.SubmissionSessionSummaryDto.builder()
                        .sessionId(s.getSessionId())
                        .sessionCode(s.getSessionCode())
                        .title(s.getTitle())
                        .content(s.getContent())
                        .subjectId(s.getSubjectId())
                        .subjectName(s.getSubjectName())
                        .subjectCode(s.getSubjectCode())
                        .courseClassId(s.getCourseClassId())
                        .classCode(s.getClassCode())
                        .semesterId(s.getSemesterId())
                        .semesterName(s.getSemesterName())
                        .status(s.getStatus())
                        .questionCounts(s.getQuestionCount())
                        .createdAt(s.getCreatedAt())
                        .reactCount(s.getReactCount())
                        .commentCount(s.getCommentCount())
                        .build())
                .toList();

        return SubmissionsResponse.builder()
                .contents(contents)
                .pagination(CursorPaginationDto.builder()
                        .after(nextAfter)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MySubmissionDetailResponse getMySubmissionDetail(Long sessionId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Session session = sessionRepository.findByIdFetchCourseClassProposerAndReviewer(sessionId)
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        if (session.getProposer() == null || !session.getProposer().getId().equals(currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        List<Question> questions = questionRepository.findQuestionsBySessionId(sessionId);
        String reviewerName = null;
        if (session.getReviewer() != null) {
            reviewerName = userProfileRepository.findById(session.getReviewer().getId())
                    .map(UserProfile::getFullName)
                    .filter(fullName -> !fullName.isBlank())
                    .orElse(session.getReviewer().getEmail());
        }

        List<MySubmissionDetailResponse.MySubmissionQuestionDto> questionDtos = questions.stream()
                .map(q -> {

                    List<String> imageUrls = q.getOwnedMedias() != null ? q.getOwnedMedias().stream()
                            .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                            .map(QuestionMedia::getUrl)
                            .toList() : List.of();

                    return MySubmissionDetailResponse.MySubmissionQuestionDto.builder()
                            .questionId(q.getId())
                            .content(q.getContent())
                            .imageUrls(imageUrls)
                            .options(q.getOptions())
                            .explanation(q.getExplanation())
                            .source(q.isLlmGenerated() ? QuestionSource.LLM : QuestionSource.HOMO_SAPIENS)
                            .confidenceScore(q.getConfidenceScore())
                            .commentCount(q.getCommentCount() != null ? q.getCommentCount() : 0)
                            .reactCount(q.getReactCount() != null ? q.getReactCount() : 0)
                            .ratingCount(q.getRatingCount() != null ? q.getRatingCount() : 0)
                            .build();
                }).toList();

        CourseClass courseClass = session.getCourseClass();
        Subject subject = courseClass.getSubject();
        Semester semester = courseClass.getSemester();

        return MySubmissionDetailResponse.builder()
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .title(session.getTitle())
                .content(session.getContent())
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .subjectCode(subject != null ? subject.getCode() : null)
                .courseClassId(courseClass.getId())
                .classCode(courseClass.getClassCode())
                .semesterId(semester.getId())
                .semesterName(semester.getName())
                .status(session.getStatus())
                .reviewedByLecturer(reviewerName)
                .reviewedAt(session.getReviewedAt())
                .commentCount(session.getCommentCount())
                .reactCount(session.getReactCount())
                .createdAt(session.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    private Session getSession(Long userId, Long sessionId) {

        Session session = sessionRepository.findByIdFetchCourseClass(sessionId)
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        if (session.getProposer() == null || !session.getProposer().getId().equals(userId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        return session;
    }

    @Override
    @Transactional
    public void updateMySubmissionSession(Long sessionId, UpdateSubmissionSessionRequest request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Session session = getSession(currentUserId, sessionId);

        // Không thể sửa nếu đang là PENDING
        if (session.getStatus() != SessionStatus.PENDING) {
            throw new AppException(ResponseCode.ACCESS_DENIED,
                    "Phiên đề xuất đã được hệ thống tiếp nhận xử lý, không thể chỉnh sửa");
        }

        if (request.title() != null && !request.title().isBlank()) {
            session.setTitle(request.title().trim());
        }

        if (request.content() != null) {
            session.setContent(request.content());
        }

        if (request.sourceUrl() != null) {
            session.setSourceUrl(request.sourceUrl());
        }

        if (request.courseClassId() != null) {
            CourseClass courseClass = resolveAuthorizedActiveCourseClass(
                    currentUserId, session.getProposer().getRole(), request.courseClassId());
            session.setCourseClass(courseClass);
        }

        // Không đánh index, đang PENDING
        Session savedSession = sessionRepository.save(session);

        // Cập nhật/Thêm/Xóa danh sách câu hỏi nếu DTO gửi lên câu hỏi
        if (request.questions() != null) {
            List<Question> existingQuestions = questionRepository.findBySessionId(sessionId);
            Map<Long, Question> existingMap = existingQuestions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));

            Set<Long> processedQuestionIds = new HashSet<>();
            List<Question> questionsToSave = new ArrayList<>();

            String authorCode = anonymizerUtil.encodeUserId(currentUserId);
            Subject finalSubject = savedSession.getCourseClass().getSubject();

            List<UpdateSubmissionSessionRequest.QuestionUpdateDto> dtoList = request.questions();
            int size = dtoList.size();
            for (int i = 0; i < size; i++) {
                UpdateSubmissionSessionRequest.QuestionUpdateDto qDto = dtoList.get(i);
                Question q;

                if (qDto.questionId() != null && existingMap.containsKey(qDto.questionId())) {
                    q = existingMap.get(qDto.questionId());
                    processedQuestionIds.add(q.getId());

                    String content = qDto.content();
                    List<QuestionOption> options = qDto.options();
                    String explanation = qDto.explanation();

                    if (content != null) {
                        q.setContent(content);
                    }

                    if (options != null) {
                        q.setOptions(options);
                    }

                    if (explanation != null) {
                        q.setExplanation(explanation);
                    }

                    boolean llmGenerated = qDto.source() != QuestionSource.HOMO_SAPIENS;

                    q.setLlmGenerated(llmGenerated);
                    if (qDto.confidence() != null) {
                        q.setConfidenceScore(qDto.confidence());
                    }
                    q.setDisplayOrder(i + 1);
                    q.setUpdatedAt(Instant.now());
                } else {
                    String textContent = qDto.content() != null ? qDto.content() : "";
                    boolean llmGenerated = qDto.source() != QuestionSource.HOMO_SAPIENS;

                    Instant now = Instant.now();
                    q = Question.builder()
                            .session(savedSession)
                            .content(textContent)
                            .difficulty(QuestionDifficulty.UNCLASSIFIED)
                            .status(QuestionStatus.PENDING)
                            .options(qDto.options())
                            .explanation(qDto.explanation())
                            .llmGenerated(llmGenerated)
                            .confidenceScore(qDto.confidence() != null ? qDto.confidence() : 0.0)
                            .displayOrder(i + 1)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                }

                questionsToSave.add(q);
            }

            List<Question> questionsToDelete = existingQuestions.stream()
                    .filter(q -> !processedQuestionIds.contains(q.getId()))
                    .toList();

            if (!questionsToDelete.isEmpty()) {
                counterMetricsService.incrementProposedQuestions(currentUserId, -questionsToDelete.size());
                questionRepository.deleteAll(questionsToDelete);
            }

            // Không đánh index (đang PENDING)
            List<Question> savedQuestions = questionRepository.saveAll(questionsToSave);

            for (Question q : savedQuestions) {
                if (q.getQuestionCode() == null || q.getQuestionCode().isBlank()) {
                    q.setQuestionCode(finalSubject.getCode() + "_" + authorCode + "_" + q.getId());
                }
            }

            duplicateDetectionService.asyncCheckDuplicates(sessionId);
        }
    }

    @Override
    @Transactional
    public void deleteMySubmissionSession(Long sessionId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Session session = getSession(currentUserId, sessionId);

        // Không xóa PENDING
        if (session.getStatus() != SessionStatus.PENDING) {
            throw new AppException(ResponseCode.ACCESS_DENIED,
                    "Phiên đề xuất đã được hệ thống tiếp nhận xử lý, không thể xóa");
        }

        List<Question> questions = questionRepository.findBySessionId(sessionId);
        if (!questions.isEmpty()) {
            List<Long> qIds = questions.stream().map(Question::getId).toList();
            List<QuestionMedia> medias = questionMediaRepository.findByQuestionIdIn(qIds);
            List<String> mediaUrlsToDelete = (medias != null) ? medias.stream()
                    .map(QuestionMedia::getUrl)
                    .filter(Objects::nonNull)
                    .toList() : Collections.emptyList();

            counterMetricsService.incrementProposedQuestions(currentUserId, -questions.size());
            if (medias != null && !medias.isEmpty()) {
                questionMediaRepository.deleteAll(medias);
            }
            questionRepository.deleteAll(questions);

            if (!mediaUrlsToDelete.isEmpty()) {
                eventPublisher.publishEvent(MediaCleanupEvent.of(mediaUrlsToDelete));
            }
        }

        sessionRepository.delete(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseClassOptionResponse> getMyEnrolledCourseClasses() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        Semester activeSemester = currentSemesterHolder.getCurrentSemester();
        List<UserCourseClass> enrollments = userCourseClassRepository.findByUserIdFetchCourseClassAndSubject(userId);

        return enrollments.stream()
                .map(UserCourseClass::getCourseClass)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((CourseClass cc) -> !isActiveSemester(cc, activeSemester))
                        .thenComparing(cc -> cc.getSemester().getName(), Comparator.reverseOrder())
                        .thenComparing(cc -> cc.getSubject().getName())
                        .thenComparing(CourseClass::getClassCode))
                .map(cc -> {
                    boolean active = isActiveSemester(cc, activeSemester);
                    return CourseClassOptionResponse.builder()
                            .courseClassId(cc.getId())
                            .classCode(cc.getClassCode())
                            .subjectId(cc.getSubject().getId())
                            .subjectCode(cc.getSubject().getCode())
                            .subjectName(cc.getSubject().getName())
                            .semesterId(cc.getSemester().getId())
                            .semesterName(cc.getSemester().getName())
                            .activeSemester(active)
                            .canPropose(active)
                            .build();
                })
                .toList();
    }

    private CourseClass resolveAuthorizedActiveCourseClass(Long userId, UserRole role, Long courseClassId) {
        if (courseClassId == null) {
            throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER,
                    "Lớp học phần không được để trống");
        }
        CourseClass courseClass = courseClassRepository.findByIdFetchAll(courseClassId)
                .orElseThrow(() -> new AppException(ResponseCode.COURSE_CLASS_NOT_FOUND));

        Semester activeSemester = currentSemesterHolder.getCurrentSemester();
        if (activeSemester == null) {
            throw new AppException(ResponseCode.NO_ACTIVE_SEMESTER, "Không có học kỳ nào đang mở trong hệ thống");
        }
        if (!Objects.equals(courseClass.getSemester().getId(), activeSemester.getId())) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    "Chỉ có thể đề xuất câu hỏi cho lớp học phần thuộc học kỳ hiện tại");
        }

        boolean authorized = switch (role) {
            case ADMIN -> true;
            case LECTURER -> courseClass.getLecturer() != null
                    && Objects.equals(courseClass.getLecturer().getId(), userId);
            case STUDENT -> userCourseClassRepository.existsByIdUserIdAndIdCourseClassId(userId, courseClassId);
        };
        if (!authorized) {
            throw new AppException(ResponseCode.ACCESS_DENIED,
                    "Bạn không được phân công hoặc chưa tham gia lớp học phần đã chọn");
        }
        return courseClass;
    }

    private boolean isActiveSemester(CourseClass courseClass, Semester activeSemester) {
        return activeSemester != null
                && courseClass.getSemester() != null
                && Objects.equals(courseClass.getSemester().getId(), activeSemester.getId());
    }

}
