package com.frozenheart.backend.modules.ai.scheduler;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.session.DuplicateCheckStatus;
import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.QuestionRejectionCode;
import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.modules.session.service.impl.QuestionReviewServiceImpl;
import com.frozenheart.backend.modules.session.service.impl.QuestionDuplicateComparison;
import com.frozenheart.backend.modules.session.service.impl.QuestionDuplicateComparison.ExactIdentity;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.notification.NotificationCategory;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
import com.frozenheart.backend.modules.notification.service.NotificationWriter.PushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Luồng tự động hóa kiểm duyệt phiên đề xuất sau 3 ngày & 6 ngày nếu không có
 * tác động từ Giảng viên.
 * Tối ưu hóa 100%: Sử dụng JOIN FETCH tránh N+1 Query và thực hiện Batch Save /
 * Batch Reward ở cuối chu trình.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAutomatedReviewScheduler {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final PostRepository postRepository;
    private final NotificationWriter notificationWriter;
    private final CounterMetricsService counterMetricsService;
    private final GamificationService gamificationService;
    private final ActivityFeedService activityFeedService;
    private final AnonymizerUtil anonymizerUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 2 * * *", zone = Time.DEFAULT_TIMEZONE) // Chạy hàng ngày vào lúc 2:00 AM (Asia/Ho_Chi_Minh)
    @Transactional
    public void runAutomatedFallbackReviewWorkflow() {
        Instant now = Instant.now();
        Instant sixDaysAgo = now.minus(6, ChronoUnit.DAYS);
        List<Session> targetSessions = loadReviewBatch(sixDaysAgo, PageRequest.of(0, 50));
        Map<Long, Set<ExactIdentity>> approvedKeys = loadApprovedKeys(targetSessions);

        List<Question> allQuestionsToUpdate = new ArrayList<>();
        List<Long> finalizedQuestionIds = new ArrayList<>();
        List<Session> allSessionsToUpdate = new ArrayList<>();
        Map<Session, List<Question>> finalizedQuestionsBySession = new LinkedHashMap<>();
        Map<Semester, List<UserPointRewardDto>> rewardsBySemester = new LinkedHashMap<>();
        List<Post> autoPostsToSave = new ArrayList<>();
        List<PushMessage> autoNotificationsToSave = new ArrayList<>();

        for (Session session : targetSessions.stream().sorted(Comparator.comparing(Session::getId)).toList()) {
            List<Question> questionsList = session.getQuestions() != null ? new ArrayList<>(session.getQuestions())
                    : List.of();
            if (questionsList.isEmpty()) {
                continue;
            }

            int approvedInSession = 0;
            Set<ExactIdentity> subjectKeys = approvedKeys.computeIfAbsent(subjectId(session), ignored -> new HashSet<>());
            for (Question question : questionsList) {
                if (question.getStatus() == QuestionStatus.APPROVED) {
                    approvedInSession++;
                    continue;
                }
                if (question.getStatus() != QuestionStatus.PENDING
                        || question.getDuplicateCheckStatus() != DuplicateCheckStatus.COMPLETE) continue;

                boolean exactDuplicate = question.getDuplicateWarnings() != null && question.getDuplicateWarnings().stream()
                        .anyMatch(warning -> warning.kind() == DuplicateWarning.Kind.EXACT);
                ExactIdentity identity = QuestionDuplicateComparison.exactIdentity(question);
                exactDuplicate |= !identity.content().isBlank() && subjectKeys.contains(identity);
                if (exactDuplicate) {
                    question.setStatus(QuestionStatus.REJECTED);
                    question.setRejectionCode(QuestionRejectionCode.DUPLICATE);
                    question.setRejectionReason("Hệ thống phát hiện bản sao chính xác.");
                } else {
                    question.setDifficulty(QuestionDifficulty.UNCLASSIFIED);
                    question.setStatus(QuestionStatus.APPROVED);
                    if (!identity.content().isBlank()) subjectKeys.add(identity);
                    approvedInSession++;
                }
                question.setUpdatedAt(now);
                allQuestionsToUpdate.add(question);
            }

            if (questionsList.stream().anyMatch(question -> question.getStatus() == QuestionStatus.PENDING)) continue;
            questionsList.stream().map(Question::getId).forEach(finalizedQuestionIds::add);
            session.setStatus(SessionStatus.RESOLVED);
            session.setReviewedAt(now);
            allSessionsToUpdate.add(session);

            finalizedQuestionsBySession.put(session, questionsList);

            // Thưởng điểm & Tự động đăng bài/feed nếu có ít nhất 1 câu hỏi được duyệt
            if (approvedInSession > 0 && session.getProposer() != null) {
                User proposer = session.getProposer();
                double totalPoints = approvedInSession * Point.APPROVED_QUESTION.getPoints();

                rewardsBySemester.computeIfAbsent(session.getCourseClass().getSemester(), _ -> new ArrayList<>())
                        .add(UserPointRewardDto.builder()
                        .userId(proposer.getId())
                        .pointsDelta(totalPoints)
                        .approvedDelta(approvedInSession)
                        .reason(PointHistoryReason.AUTOMATED_SYSTEM_RESOLVE_BONUS)
                        .targetType(PointHistoryTargetType.SESSION)
                        .targetId(session.getId())
                        .subject(session.getCourseClass().getSubject())
                        .build());

                Subject subject = session.getCourseClass().getSubject();
                String subjectName = subject != null ? subject.getName() : "Môn học";
                String subjectCode = subject != null ? subject.getCode() : "Mã môn";
                String proposerName = anonymizerUtil.encodeUserId(proposer.getId());

                QuestionReviewServiceImpl.createPostAndAddFeed(autoPostsToSave, session, proposer, subject, subjectName,
                        subjectCode, proposerName, now, activityFeedService);

                // 1. Tạo thông báo nội bộ (Internal Notification - không tiết lộ số lượng câu)
                Notification notif = Notification.builder()
                        .receiver(proposer)
                        .actor(null) // Hệ thống tự động duyệt
                        .type(NotificationType.QUESTION_APPROVED)
                        .category(NotificationCategory.ACADEMIC)
                        .title("Câu hỏi của bạn đã được phê duyệt tự động!")
                        .body("Phiên đề xuất môn [" + subjectName + "] (" + subjectCode
                                + ") đã có câu hỏi được hệ thống phê duyệt.")
                        .targetType(NotificationTargetType.SESSION)
                        .targetId(session.getId())
                        .creadtedAt(now)
                        .build();
                autoNotificationsToSave.add(new PushMessage(notif,
                        "Câu hỏi của bạn đã được phê duyệt!",
                        "Phiên đề xuất môn [" + subjectName + "] đã có câu hỏi được chấp nhận vào Ngân hàng câu hỏi.",
                        "/sessions/" + session.getId()));
            }
        }

        // Thực hiện Batch Save 1 lần duy nhất cho toàn bộ danh sách
        if (!allQuestionsToUpdate.isEmpty()) {
            questionRepository.saveAll(allQuestionsToUpdate);
        }
        if (!allSessionsToUpdate.isEmpty()) {
            sessionRepository.saveAll(allSessionsToUpdate);
            gamificationService.onSessionsFinalized(finalizedQuestionsBySession);
            List<Long> sIds = allSessionsToUpdate.stream().map(Session::getId).toList();
            eventPublisher
                    .publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.SESSION, sIds));
            eventPublisher.publishEvent(EntitySearchSyncEvent.upsertBatch(
                    EntitySearchSyncEvent.EntityType.QUESTION, finalizedQuestionIds));
        }
        if (!autoPostsToSave.isEmpty()) {
            List<Post> savedPosts = postRepository.saveAll(autoPostsToSave);
            List<Long> pIds = savedPosts.stream().map(Post::getId).toList();
            eventPublisher.publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.POST, pIds));
        }
        notificationWriter.createAllWithPush(autoNotificationsToSave, PushNotificationType.ACADEMIC);
        rewardsBySemester.forEach((semester, rewards) ->
                counterMetricsService.awardPointsAndApprovedQuestionsBatch(rewards, semester));

        log.info("Automated AI review fallback workflow completed successfully.");
    }

    private Map<Long, Set<ExactIdentity>> loadApprovedKeys(List<Session> sessions) {
        Map<Long, Set<ExactIdentity>> result = new LinkedHashMap<>();
        List<Long> ids = sessions.stream().map(AiAutomatedReviewScheduler::subjectId)
                .filter(Objects::nonNull).distinct().sorted().toList();
        if (ids.isEmpty()) return result;
        subjectRepository.lockForAutoReview(ids);
        for (Question question : questionRepository.findApprovedForAutoReview(ids)) {
            ExactIdentity identity = QuestionDuplicateComparison.exactIdentity(question);
            if (!identity.content().isBlank()) {
                result.computeIfAbsent(subjectId(question.getSession()), ignored -> new HashSet<>()).add(identity);
            }
        }
        return result;
    }

    private static Long subjectId(Session session) {
        return session.getCourseClass() == null || session.getCourseClass().getSubject() == null ? null
                : session.getCourseClass().getSubject().getId();
    }

    private List<Session> loadReviewBatch(Instant cutoff, Pageable batchLimit) {
        List<Long> sessionIds = sessionRepository.findReadyForAutoReview(cutoff, batchLimit);
        if (sessionIds.isEmpty()) return List.of();
        sessionIds = sessionRepository.lockReviewSessions(sessionIds, true);
        return sessionIds.isEmpty() ? List.of() : sessionRepository.findByIdInFetchAutomatedReviewDetails(sessionIds);
    }
}
