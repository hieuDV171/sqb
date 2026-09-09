package com.frozenheart.backend.modules.ai.scheduler;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.service.AiIntegrationService;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.modules.session.service.impl.QuestionReviewServiceImpl;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import com.frozenheart.backend.core.dto.event.PushNotificationEvent;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.notification.NotificationCategory;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
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
import java.util.List;

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
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final AiIntegrationService aiIntegrationService;
    private final CounterMetricsService counterMetricsService;
    private final GamificationService gamificationService;
    private final ActivityFeedService activityFeedService;
    private final AnonymizerUtil anonymizerUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 2 * * *", zone = Time.DEFAULT_TIMEZONE) // Chạy hàng ngày vào lúc 2:00 AM (Asia/Ho_Chi_Minh)
    @Transactional
    public void runAutomatedFallbackReviewWorkflow() {
        log.info("Starting 3-day and 6-day automated AI review fallback workflow...");

        Instant now = Instant.now();
        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        Instant sixDaysAgo = now.minus(6, ChronoUnit.DAYS);

        Pageable batchLimit = PageRequest.of(0, 50);

        // ==========================================
        // GIAI ĐOẠN 1: TỰ ĐỘNG AI REFINEMENT SAU 3 NGÀY
        // ==========================================
        List<Session> pendingSessionsThreeDays = sessionRepository
                .findByStatusAndCreatedAtBeforeFetchDetails(SessionStatus.PENDING, threeDaysAgo, batchLimit);

        for (Session session : pendingSessionsThreeDays) {
            if (session.getQuestions() == null)
                continue;
            for (Question q : session.getQuestions()) {
                // Nếu câu hỏi có cảnh báo trùng lặp
                if (q.getDuplicateWarnings() != null && !q.getDuplicateWarnings().isEmpty()) {
                    try {
                        aiIntegrationService.refineQuestion(
                                q.getId(),
                                new AiRefineRequest(
                                        "Câu hỏi gốc bị cảnh báo trùng lặp với câu hỏi trong ngân hàng đề, bạn hãy sửa câu hỏi sao cho vẫn giữ được tính thần cốt lõi của câu hỏi",
                                        null),
                                EditActorType.SYSTEM);
                    } catch (Exception e) {
                        log.warn("Failed to auto-refine question ID {}: {}", q.getId(), e.getMessage());
                    }
                }
            }
        }

        // ==========================================
        // GIAI ĐOẠN 2: TỰ ĐỘNG CHẤP NHẬN / TỪ CHỐI SAU 6 NGÀY
        // ==========================================
        List<Session> targetSessions = sessionRepository
                .findByStatusAndCreatedAtBeforeFetchDetails(SessionStatus.PENDING, sixDaysAgo, batchLimit);

        List<Question> allQuestionsToUpdate = new ArrayList<>();
        List<Session> allSessionsToUpdate = new ArrayList<>();
        List<UserPointRewardDto> rewardsToBatch = new ArrayList<>();
        List<Post> autoPostsToSave = new ArrayList<>();
        List<Notification> autoNotificationsToSave = new ArrayList<>();

        for (Session session : targetSessions) {
            List<Question> questionsList = session.getQuestions() != null ? new ArrayList<>(session.getQuestions())
                    : List.of();
            if (questionsList.isEmpty()) {
                continue;
            }

            int approvedInSession = 0;
            for (Question question : questionsList) {
                if (question.getStatus() == QuestionStatus.APPROVED) {
                    approvedInSession++;
                    allQuestionsToUpdate.add(question);
                    continue;
                }

                if (question.getStatus() == QuestionStatus.PENDING) {
                    boolean hasDuplicateWarning = question.getDuplicateWarnings() != null
                            && !question.getDuplicateWarnings().isEmpty();
                    if (!hasDuplicateWarning) {
                        question.setStatus(QuestionStatus.APPROVED);
                        approvedInSession++;
                    } else {
                        question.setStatus(QuestionStatus.REJECTED);
                    }
                    question.setUpdatedAt(now);
                    allQuestionsToUpdate.add(question);
                }
            }

            session.setStatus(SessionStatus.RESOLVED);
            session.setReviewedAt(now);
            allSessionsToUpdate.add(session);

            gamificationService.resolveGame2And3ForSession(session, questionsList);

            // Thưởng điểm & Tự động đăng bài/feed nếu có ít nhất 1 câu hỏi được duyệt
            if (approvedInSession > 0 && session.getProposer() != null) {
                User proposer = session.getProposer();
                double totalPoints = approvedInSession * Point.APPROVED_QUESTION.getPoints();

                rewardsToBatch.add(UserPointRewardDto.builder()
                        .userId(proposer.getId())
                        .pointsDelta(totalPoints)
                        .approvedDelta(approvedInSession)
                        .reason(PointHistoryReason.AUTOMATED_SYSTEM_RESOLVE_BONUS)
                        .targetType(PointHistoryTargetType.SESSION)
                        .targetId(session.getId())
                        .subject(session.getSubject())
                        .build());

                Subject subject = session.getSubject();
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
                autoNotificationsToSave.add(notif);

                // 2. Gửi thông báo đẩy bất đồng bộ (Push Notification Event - không tiết lộ số
                // lượng câu)
                eventPublisher.publishEvent(PushNotificationEvent.single(
                        proposer.getId(),
                        "Câu hỏi của bạn đã được phê duyệt!",
                        "Phiên đề xuất môn [" + subjectName + "] đã có câu hỏi được chấp nhận vào Ngân hàng câu hỏi.",
                        null,
                        NotificationTargetType.SESSION,
                        session.getId(),
                        "/sessions/" + session.getId(),
                        PushNotificationType.ACADEMIC));
            }
        }

        // Thực hiện Batch Save 1 lần duy nhất cho toàn bộ danh sách
        if (!allQuestionsToUpdate.isEmpty()) {
            questionRepository.saveAll(allQuestionsToUpdate);
            List<Long> qIds = allQuestionsToUpdate.stream().map(Question::getId).toList();
            eventPublisher
                    .publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.QUESTION, qIds));
        }
        if (!allSessionsToUpdate.isEmpty()) {
            sessionRepository.saveAll(allSessionsToUpdate);
            List<Long> sIds = allSessionsToUpdate.stream().map(Session::getId).toList();
            eventPublisher
                    .publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.SESSION, sIds));
        }
        if (!autoPostsToSave.isEmpty()) {
            List<Post> savedPosts = postRepository.saveAll(autoPostsToSave);
            List<Long> pIds = savedPosts.stream().map(Post::getId).toList();
            eventPublisher.publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.POST, pIds));
        }
        if (!autoNotificationsToSave.isEmpty()) {
            notificationRepository.saveAll(autoNotificationsToSave);
        }
        if (!rewardsToBatch.isEmpty()) {
            counterMetricsService.awardPointsAndApprovedQuestionsBatch(rewardsToBatch);
        }

        log.info("Automated AI review fallback workflow completed successfully.");
    }
}
