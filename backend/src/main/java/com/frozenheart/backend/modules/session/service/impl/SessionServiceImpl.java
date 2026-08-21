package com.frozenheart.backend.modules.session.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
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
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.session.dto.MySubmissionDetailResponse;
import com.frozenheart.backend.modules.session.dto.MySubmissionsResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest.QuestionProposeDto;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.dto.UpdateSubmissionSessionRequest;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;
import com.frozenheart.backend.modules.session.service.SessionService;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final AnonymizerUtil anonymizerUtil;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    @Transactional
    @Override
    public ProposeSessionResponse proposeSession(ProposeSessionRequest request) {

        // Hiện tại, nội dung câu hỏi có thể có nhiều ảnh, nhưng đáp án chỉ được 1 ảnh

        // Lấy thông tin User nộp bài & Mã môn học
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        User proposer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy môn học"));

        // Mã ẩn danh 6 ký tự của tác giả
        String authorCode = anonymizerUtil.encodeUserId(userId);

        // Khởi tạo & Lưu Session
        Session session = Session.builder()
                .title(request.title())
                .content(request.content())
                .sourceUrl(request.sourceUrl())
                .proposer(proposer)
                .subject(subject)
                .status(SessionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        session = sessionRepository.save(session);

        // {subjectCode}_{authorCode}_{timestamp}_{sessionId}
        String sessionCode = subject.getCode() + "_" + authorCode + "_" + System.currentTimeMillis() + "_" + session.getId();
        session.setSessionCode(sessionCode);

        // Chuyển đổi danh sách DTO sang danh sách Question Entity
        List<QuestionProposeDto> requestedQuestions = request.questions();
        Session finalSession = session;

        List<Question> questions = requestedQuestions.stream()
                .map(q -> {
                    String textContent = q.content();
                    return Question.builder()
                            .session(finalSession) // Gán mối quan hệ với Session

                            .originalContent(textContent)
                            .content(textContent)
                            
                            .difficulty(QuestionDifficulty.UNCLASSIFIED)
                            .status(QuestionStatus.PENDING)

                            // Gán cả bản gốc và bản làm việc
                            .originalOptions(q.options())
                            .options(q.options())

                            .originalExplanation(q.explanation())
                            .explanation(q.explanation())

                            .llmGenerated(q.llmGenerated())
                            .confidenceScore(q.confidence() != null ? q.confidence() : 0.0)
                            .createdAt(LocalDateTime.now())
                            .build();
                }).toList();

        // Lưu câu hỏi vào DB
        List<Question> savedQuestions = questionRepository.saveAll(questions);

        // Cập nhật displayOrder, questionCode và Lưu QuestionMedia đính kèm
        List<QuestionMedia> mediaListToSave = new ArrayList<>();

        // Cập nhật questionCode cho từng câu hỏi
        int size = savedQuestions.size();
        for (int i = 0; i < size; i++) {
            Question q = savedQuestions.get(i);
            QuestionProposeDto dto = requestedQuestions.get(i);

            q.setDisplayOrder(i + 1);
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
                                .uploadedAt(LocalDateTime.now())
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
                                .uploadedAt(LocalDateTime.now())
                                .build();

                        mediaListToSave.add(media);
                    }
                }
            }
        }
        
        if (!mediaListToSave.isEmpty()) {
            List<QuestionMedia> savedMedias = questionMediaRepository.saveAll(mediaListToSave);
            
            // Tạo Map tra cứu siêu nhanh O(1) trên RAM: Key = questionId_mediaUrl -> Value = mediaId
            Map<String, Long> mediaIdMap = savedMedias.stream()
                    .collect(Collectors.toMap(
                            m -> m.getQuestion().getId() + "_" + m.getUrl(), // key
                            q -> q.getId(), // value
                            (existing, replacement) -> existing // xử lý đụng độ
                    ));

            // 2. Tra cứu O(1) để gán mediaId ngược lại cho từng QuestionOption
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

        // BẮN TASK CHẠY NGẦM CHECK TRÙNG 3 TẦNG
        duplicateDetectionService.asyncCheckDuplicates(session.getId());

        return ProposeSessionResponse.builder()
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .subjectId(subject.getId())
                .questionCount(request.questions().size())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MySubmissionsResponse getMySubmissions(
            Long after, Integer limit, Long subjectId, SessionStatus status) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<Session> sessions = sessionRepository.findMySubmissions(currentUserId, subjectId, status, after, pageable);

        boolean hasNext = false;
        if (sessions.size() > pageSize) {
            hasNext = true;
            sessions = sessions.subList(0, pageSize);
        }

        Long nextAfter = null;
        if (!sessions.isEmpty()) {
            nextAfter = sessions.get(sessions.size() - 1).getId();
        }

        List<MySubmissionsResponse.MySubmissionSessionSummaryDto> contents = sessions.stream()
                .map(s -> MySubmissionsResponse.MySubmissionSessionSummaryDto.builder()
                        .sessionId(s.getId())
                        .subjectId(s.getSubject() != null ? s.getSubject().getId() : null)
                        .subjectName(s.getSubject() != null ? s.getSubject().getName() : null)
                        .questionCounts(s.getQuestions() != null ? s.getQuestions().size() : 0)
                        .createdAt(s.getCreatedAt())
                        .reactCount(s.getReactCount())
                        .commentCount(s.getCommentCount())
                        .build()
                ).toList();

        return MySubmissionsResponse.builder()
                .contents(contents)
                .pagination(MySubmissionsResponse.CursorPaginationDto.builder()
                        .after(nextAfter)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MySubmissionDetailResponse getMySubmissionDetail(Long sessionId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Session session = sessionRepository.findByIdWithSubject(sessionId)
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        if (session.getProposer() == null || !session.getProposer().getId().equals(currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        List<Object[]> rows = questionRepository.findQuestionsWithReviewerFullNameBySessionId(sessionId);



        List<MySubmissionDetailResponse.MySubmissionQuestionDto> questionDtos = rows.stream()
                .map(row -> {

                    Question q = (Question) row[0];
                    String reviewerFullName = (String) row[1];

                    List<String> imageUrls = q.getOwnedMedias() != null ?
                            q.getOwnedMedias().stream()
                                    .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                                    .map(m -> m.getUrl())
                                    .toList() : List.of();

                    String reviewerName = q.getReviewer() != null ? q.getReviewer().getEmail() : null;

                    return MySubmissionDetailResponse.MySubmissionQuestionDto.builder()
                            .questionId(q.getId())
                            .content(q.getOriginalContent())
                            .imageUrls(imageUrls)
                            .options(q.getOriginalOptions())
                            .explanation(q.getOriginalExplanation())
                            .source(q.isLlmGenerated() ? "LLM" : "H")
                            .confidenceScore(q.getConfidenceScore())
                            .reviewedByLecturer(reviewerFullName != null ? reviewerFullName : reviewerName)
                            .reviewedAt(q.getReviewdAt())
                            .commentCount(q.getCommentCount() != null ? q.getCommentCount() : 0)
                            .reactCount(q.getReactCount() != null ? q.getReactCount() : 0)
                            .ratingCount(q.getRatingCount() != null ? q.getRatingCount() : 0)
                            .build();
                }).toList();

        return MySubmissionDetailResponse.builder()
                .sessionId(session.getId())
                .subjectId(session.getSubject() != null ? session.getSubject().getId() : null)
                .subjectName(session.getSubject() != null ? session.getSubject().getName() : null)
                .commentCount(session.getCommentCount())
                .reactCount(session.getReactCount())
                .createdAt(session.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    @Override
    @Transactional
    public void updateMySubmissionSession(Long sessionId, UpdateSubmissionSessionRequest request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        if (session.getProposer() == null || !session.getProposer().getId().equals(currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        // Option 1 Rule: Cannot edit if status is not PENDING
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

        if (request.subjectId() != null) {
            Subject subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy môn học"));
            session.setSubject(subject);
        }

        session = sessionRepository.save(session);

        // Cập nhật/Thêm/Xóa danh sách câu hỏi nếu DTO gửi lên câu hỏi
        if (request.questions() != null) {
            List<Question> existingQuestions = questionRepository.findBySessionId(sessionId);
            Map<Long, Question> existingMap = existingQuestions.stream()
                    .collect(Collectors.toMap(q -> q.getId(), q -> q));

            Set<Long> processedQuestionIds = new HashSet<>();
            List<Question> questionsToSave = new ArrayList<>();

            String authorCode = anonymizerUtil.encodeUserId(currentUserId);
            Subject finalSubject = session.getSubject();

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
                        q.setOriginalContent(content);
                        q.setContent(content);
                    }

                    if (options != null) {
                        q.setOriginalOptions(options);
                        q.setOptions(options);
                    }

                    if (explanation != null) {
                        q.setOriginalExplanation(explanation);
                        q.setExplanation(explanation);
                    }

                    boolean llmGenerated = qDto.source() == QuestionSource.HOMO_SAPIENS ? false : true;

                    q.setLlmGenerated(llmGenerated);
                    if (qDto.confidence() != null) {
                        q.setConfidenceScore(qDto.confidence());
                    }
                    q.setDisplayOrder(i + 1);
                } else {
                    String textContent = qDto.content() != null ? qDto.content() : "";
                    boolean llmGenerated = qDto.source() == QuestionSource.HOMO_SAPIENS ? false : true;
                    q = Question.builder()
                            .session(session)
                            .originalContent(textContent)
                            .content(textContent)
                            .difficulty(QuestionDifficulty.UNCLASSIFIED)
                            .status(QuestionStatus.PENDING)
                            .originalOptions(qDto.options())
                            .options(qDto.options())
                            .originalExplanation(qDto.explanation())
                            .explanation(qDto.explanation())
                            .llmGenerated(llmGenerated)
                            .confidenceScore(qDto.confidence() != null ? qDto.confidence() : 0.0)
                            .displayOrder(i + 1)
                            .createdAt(LocalDateTime.now())
                            .build();
                }

                questionsToSave.add(q);
            }

            List<Question> questionsToDelete = existingQuestions.stream()
                    .filter(q -> !processedQuestionIds.contains(q.getId()))
                    .toList();

            if (!questionsToDelete.isEmpty()) {
                questionRepository.deleteAll(questionsToDelete);
            }

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

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        if (session.getProposer() == null || !session.getProposer().getId().equals(currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        // Option 1 Rule: Cannot delete if status is not PENDING
        if (session.getStatus() != SessionStatus.PENDING) {
            throw new AppException(ResponseCode.ACCESS_DENIED,
                    "Phiên đề xuất đã được hệ thống tiếp nhận xử lý, không thể xóa");
        }

        List<Question> questions = questionRepository.findBySessionId(sessionId);
        if (!questions.isEmpty()) {
            questionRepository.deleteAll(questions);
        }

        sessionRepository.delete(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectList() {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .map(sub -> SubjectResponse.builder()
                        .id(sub.getId())
                        .code(sub.getCode())
                        .name(sub.getName())
                        .build())
                .toList();
    }

}
