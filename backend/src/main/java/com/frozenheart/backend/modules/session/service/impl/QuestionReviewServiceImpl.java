package com.frozenheart.backend.modules.session.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.EditQuestionRequest;
import com.frozenheart.backend.modules.session.dto.EditQuestionResponse;
import com.frozenheart.backend.modules.session.dto.PendingSessionsResponse;
import com.frozenheart.backend.modules.session.dto.RejectQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.SessionDetailReviewResponse;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.service.QuestionReviewService;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;

import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLog;
import com.frozenheart.backend.modules.session.repository.QuestionEditLogRepository;

import com.frozenheart.backend.modules.gamification.service.GamificationService;

@Service
@RequiredArgsConstructor
public class QuestionReviewServiceImpl implements QuestionReviewService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEditLogRepository questionEditLogRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CounterMetricsService counterMetricsService;
    private final AnonymizerUtil anonymizerUtil;
    private final GamificationService gamificationService;

    @Override
    @Transactional(readOnly = true)
    public PendingSessionsResponse getPendingSessions(Long after, Integer limit, Long subjectId, String sortBy) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Session> sessions = sessionRepository.findPendingSessionsFetchSubjectAndProposerWithCursor(subjectId, after, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (sessions.size() > pageSize) {
            hasNext = true;
            sessions = sessions.subList(0, pageSize);
            nextCursor = sessions.getLast().getId();
        }

        List<Long> proposerIds = sessions.stream()
                .filter(s -> s.getProposer() != null)
                .map(s -> s.getProposer().getId())
                .distinct()
                .toList();

        Map<Long, UserProfile> profileMap = proposerIds.isEmpty() ? Map.of() :
                userProfileRepository.findAllById(proposerIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<PendingSessionsResponse.PendingSessionItemDto> items = sessions.stream().map(s -> {
            User proposer = s.getProposer();
            UserProfile profile = proposer != null ? profileMap.get(proposer.getId()) : null;
            String anonymizedName = proposer != null ? "Học viên " + anonymizerUtil.encodeUserId(proposer.getId()) : "Học viên ẩn danh";

            PendingSessionsResponse.AuthorDto authorDto = PendingSessionsResponse.AuthorDto.builder()
                    .userId(proposer != null ? proposer.getId() : null)
                    .fullName(anonymizedName)
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .build();

            return PendingSessionsResponse.PendingSessionItemDto.builder()
                    .sessionId(s.getId())
                    .topic(s.getSubject() != null ? s.getSubject().getName() : null)
                    .title(s.getTitle())
                    .content(s.getContent())
                    .author(authorDto)
                    .status(s.getStatus() != null ? s.getStatus().name() : "PENDING")
                    .createdAt(s.getCreatedAt())
                    .build();
        }).toList();

        return PendingSessionsResponse.builder()
                .items(items)
                .pagination(CursorPaginationDto.builder()
                        .after(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public SessionDetailReviewResponse getSessionDetailForReview(Long sessionId) {

        JwtPayload payload = JwtPayload.getCurrentUserPayload();

        Long currentUserId = payload.getUserId();
        String role = payload.getRole();

        if (UserRole.STUDENT.name().equalsIgnoreCase(role)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Sinh viên không thể duyệt câu hỏi");
        }

        User lecturer = userRepository.getReferenceById(currentUserId);

        Session session = sessionRepository.findByIdFetchSubjectAndQuestions(sessionId)
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        if (session.getStatus() == SessionStatus.PENDING) {
            session.setStatus(SessionStatus.REVIEWING);
            session.setReviewer(lecturer);
            session.setReviewedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        List<Question> questions = session.getQuestions() != null ?
                session.getQuestions().stream()
                        .sorted(Comparator.comparingInt(Question::getDisplayOrder))
                        .toList() : List.of();

        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, QuestionEditLog> editLogMap = questionIds.isEmpty() ? Map.of() :
                questionEditLogRepository.findByQuestionIdInFetchActorOrderByCreatedAtDesc(questionIds).stream()
                        .collect(Collectors.toMap(
                                log -> log.getQuestion().getId(),
                                log -> log,
                                (existing, replacement) -> existing
                        ));

        List<Long> actorUserIds = editLogMap.values().stream()
                .filter(l -> l.getActor() != null)
                .map(l -> l.getActor().getId())
                .distinct()
                .toList();

        Map<Long, String> actorNameMap = actorUserIds.isEmpty() ? Map.of() :
                userProfileRepository.findAllById(actorUserIds).stream()
                        .collect(Collectors.toMap(
                                UserProfile::getUserId,
                                UserProfile::getFullName
                        ));

        List<SessionDetailReviewResponse.SessionQuestionReviewDto> items = questions.stream().map(q -> {
            List<String> imageUrls = q.getOwnedMedias() != null ?
                    q.getOwnedMedias().stream()
                            .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                            .map(QuestionMedia::getUrl)
                            .toList() : List.of();

            List<String> correctKeys = q.getOptions() != null ?
                    q.getOptions().stream()
                            .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                            .map(QuestionOption::getKey)
                            .toList() : List.of();

            String correctAnswerStr = String.join(", ", correctKeys);

            QuestionEditLog log = editLogMap.get(q.getId());
            SessionDetailReviewResponse.QuestionEditLogDto editLogDto = null;
            if (log != null) {
                String actorName = (log.getActorType() == EditActorType.SYSTEM || log.getActor() == null)
                        ? "Hệ thống AI (Tự động sau 3 ngày)"
                        : actorNameMap.getOrDefault(log.getActor().getId(), "Giảng viên");

                editLogDto = SessionDetailReviewResponse.QuestionEditLogDto.builder()
                        .id(log.getId())
                        .actorType(log.getActorType() != null ? log.getActorType().name() : "SYSTEM")
                        .actorName(actorName)
                        .status(log.getStatus() != null ? log.getStatus().name() : "PROPOSED")
                        .beforeState(log.getBeforeState())
                        .afterState(log.getAfterState())
                        .hallucinationAudit(log.getHallucinationAudit())
                        .createdAt(log.getCreatedAt())
                        .build();
            }

            return SessionDetailReviewResponse.SessionQuestionReviewDto.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .imageUrls(imageUrls)
                    .options(q.getOptions())
                    .correctAnswer(correctAnswerStr)
                    .explanation(q.getExplanation())
                    .status(q.getStatus() != null ? q.getStatus().name() : "PENDING")
                    .editLog(editLogDto)
                    .build();
        }).toList();

        List<DuplicateWarning> allWarnings = questions.stream()
                .filter(q -> q.getDuplicateWarnings() != null)
                .flatMap(q -> q.getDuplicateWarnings().stream())
                .toList();

        return SessionDetailReviewResponse.builder()
                .items(items)
                .duplicateWarnings(allWarnings)
                .build();
    }

    @Override
    @Transactional
    public ApproveQuestionsResponse approveQuestions(ApproveQuestionsRequest request) {
        LocalDateTime now = LocalDateTime.now();

        List<Question> questions = questionRepository.findAllById(request.questionIds());
        Set<Session> parentSessions = new HashSet<>();

        for (Question q : questions) {
            q.setStatus(QuestionStatus.APPROVED);
            q.setUpdatedAt(now);
            if (q.getSession() != null) {
                parentSessions.add(q.getSession());
            }
        }

        questionRepository.saveAll(questions);

        checkAndAutoResolveSessions(parentSessions);

        return ApproveQuestionsResponse.builder()
                .pointsEarned(0.5)
                .build();
    }

    @Override
    @Transactional
    public void rejectQuestions(RejectQuestionsRequest request) {
        LocalDateTime now = LocalDateTime.now();

        List<Question> questions = questionRepository.findAllById(request.questionIds());
        Set<Session> parentSessions = new HashSet<>();

        for (Question q : questions) {
            q.setStatus(QuestionStatus.REJECTED);
            q.setUpdatedAt(now);
            if (q.getSession() != null) {
                parentSessions.add(q.getSession());
            }
        }

        questionRepository.saveAll(questions);

        checkAndAutoResolveSessions(parentSessions);
    }

    @Override
    @Transactional
    public EditQuestionResponse editQuestion(Long questionId, EditQuestionRequest request) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String role = payload.getRole();

        if (UserRole.STUDENT.name().equals(role)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Chỉ giảng viên mới được phép sửa bất kỳ câu hỏi nào");
        }

        User lecturer = userRepository.getReferenceById(currentUserId);
        LocalDateTime now = LocalDateTime.now();

        Question question = questionRepository.findByIdFetchSessionAndSubject(questionId)
                .orElseThrow(() -> new AppException(ResponseCode.QUESTION_NOT_FOUND));

        if (request.content() != null) {
            question.setContent(request.content());
        }
        if (request.options() != null) {
            question.setOptions(request.options());
        }
        if (request.explanation() != null) {
            question.setExplanation(request.explanation());
        }

        if (Boolean.TRUE.equals(request.autoApprove())) {
            question.setStatus(QuestionStatus.APPROVED);
        } else {
            question.setStatus(QuestionStatus.PENDING);
        }

        question.setUpdatedAt(now);
        questionRepository.save(question);

        if (question.getSession() != null) {
            Session s = question.getSession();
            if (s.getReviewer() == null) {
                s.setReviewer(lecturer);
                s.setReviewedAt(now);
            }
            checkAndAutoResolveSessions(Set.of(s));
        }

        return EditQuestionResponse.builder()
                .questionId(question.getId())
                .status(question.getStatus() != null ? question.getStatus().name() : "EDITING")
                .reviewedBy(lecturer.getId())
                .reviewedAt(now)
                .build();
    }

    private void checkAndAutoResolveSessions(Set<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) return;

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();
        List<Question> allQuestions = questionRepository.findBySessionIdIn(sessionIds);

        Map<Long, List<Question>> questionsBySessionMap = allQuestions.stream()
                .filter(q -> q.getSession() != null)
                .collect(Collectors.groupingBy(q -> q.getSession().getId()));
        List<Session> sessionsToSave = new ArrayList<>();
        List<UserPointRewardDto> rewardsToBatch = new ArrayList<>();

        for (Session s : sessions) {
            List<Question> qList = questionsBySessionMap.getOrDefault(s.getId(), List.of());
            boolean allResolved = !qList.isEmpty() && qList.stream().allMatch(q ->
                    q.getStatus() == QuestionStatus.APPROVED || q.getStatus() == QuestionStatus.REJECTED);

            if (allResolved && s.getStatus() != SessionStatus.RESOLVED) {
                s.setStatus(SessionStatus.RESOLVED);
                sessionsToSave.add(s);
                
                gamificationService.resolveGame2And3ForSession(s, qList);

                boolean hasApprovedQuestion = qList.stream().anyMatch(q -> q.getStatus() == QuestionStatus.APPROVED);
                if (hasApprovedQuestion && s.getProposer() != null) {
                    User proposer = s.getProposer();
                    int approvedCount = (int) qList.stream().filter(q -> q.getStatus() == QuestionStatus.APPROVED).count();
                    Double totalPoints = approvedCount * Point.APPROVED_QUESTION.getPoints();

                    rewardsToBatch.add(UserPointRewardDto.builder()
                            .userId(proposer.getId())
                            .pointsDelta(totalPoints)
                            .approvedDelta(approvedCount)
                            .reason("SESSION_RESOLVED_BONUS")
                            .targetType("SESSION")
                            .targetId(s.getId())
                            .subject(s.getSubject())
                            .build());
                }
            }
        }

        if (!sessionsToSave.isEmpty()) {
            sessionRepository.saveAll(sessionsToSave);
        }
        if (!rewardsToBatch.isEmpty()) {
            counterMetricsService.awardPointsAndApprovedQuestionsBatch(rewardsToBatch);
        }
    }
}
