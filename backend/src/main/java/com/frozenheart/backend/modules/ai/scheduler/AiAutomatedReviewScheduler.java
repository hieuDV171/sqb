package com.frozenheart.backend.modules.ai.scheduler;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.service.AiIntegrationService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Luồng tự động hóa kiểm duyệt phiên đề xuất sau 3 ngày & 6 ngày nếu không có tác động từ Giảng viên.
 * Tối ưu hóa 100%: Sử dụng JOIN FETCH tránh N+1 Query và thực hiện Batch Save / Batch Reward ở cuối chu trình.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAutomatedReviewScheduler {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AiIntegrationService aiIntegrationService;
    private final CounterMetricsService counterMetricsService;

    @Scheduled(cron = "0 0 2 * * *") // Chạy hàng ngày vào lúc 2:00 AM
    @Transactional
    public void runAutomatedFallbackReviewWorkflow() {
        log.info("Starting 3-day and 6-day automated AI review fallback workflow...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);
        LocalDateTime sixDaysAgo = now.minusDays(6);

        Pageable batchLimit = PageRequest.of(0, 50);

        // ==========================================
        // GIAI ĐOẠN 1: TỰ ĐỘNG AI REFINEMENT SAU 3 NGÀY
        // ==========================================
        List<Session> pendingSessionsThreeDays = sessionRepository.findByStatusAndCreatedAtBeforeFetchDetails(SessionStatus.PENDING, threeDaysAgo, batchLimit);

        for (Session session : pendingSessionsThreeDays) {
            if (session.getQuestions() == null) continue;
            for (Question q : session.getQuestions()) {
                // Nếu câu hỏi có cảnh báo trùng lặp
                if (q.getDuplicateWarnings() != null && !q.getDuplicateWarnings().isEmpty()) {
                    try {
                        aiIntegrationService.refineQuestion(
                                q.getId(),
                                new AiRefineRequest("Tự động tinh chỉnh chuẩn hóa câu hỏi bị cảnh báo trùng lặp", null),
                                EditActorType.SYSTEM
                        );
                    } catch (Exception e) {
                        log.warn("Failed to auto-refine question ID {}: {}", q.getId(), e.getMessage());
                    }
                }
            }
        }

        // ==========================================
        // GIAI ĐOẠN 2: TỰ ĐỘNG CHẤP NHẬN / TỪ CHỐI SAU 6 NGÀY
        // ==========================================
        List<Session> pendingSessionsSixDays = sessionRepository.findByStatusAndCreatedAtBeforeFetchDetails(SessionStatus.PENDING, sixDaysAgo, batchLimit);

        List<Question> allQuestionsToUpdate = new ArrayList<>();
        List<Session> allSessionsToUpdate = new ArrayList<>();
        List<UserPointRewardDto> rewardsToBatch = new ArrayList<>();

        for (Session session : pendingSessionsSixDays) {
            if (session.getQuestions() == null || session.getQuestions().isEmpty()) continue;

            int approvedInSession = 0;
            for (Question q : session.getQuestions()) {
                boolean hasDuplicateWarning = (q.getDuplicateWarnings() != null && !q.getDuplicateWarnings().isEmpty());

                if (!hasDuplicateWarning) {
                    q.setStatus(QuestionStatus.APPROVED);
                    approvedInSession++;
                } else {
                    q.setStatus(QuestionStatus.REJECTED);
                }
                q.setUpdatedAt(now);
                allQuestionsToUpdate.add(q);
            }

            session.setStatus(SessionStatus.RESOLVED);
            session.setReviewedAt(now);
            allSessionsToUpdate.add(session);

            if (approvedInSession > 0 && session.getProposer() != null) {
                User proposer = session.getProposer();
                Double totalPoints = approvedInSession * Point.APPROVED_QUESTION.getPoints();

                rewardsToBatch.add(UserPointRewardDto.builder()
                        .userId(proposer.getId())
                        .pointsDelta(totalPoints)
                        .approvedDelta(approvedInSession)
                        .reason("AUTOMATED_SYSTEM_RESOLVE_BONUS")
                        .targetType("SESSION")
                        .targetId(session.getId())
                        .subject(session.getSubject())
                        .build());
            }
        }

        // Thực hiện Batch Save 1 lần duy nhất cho toàn bộ danh sách
        if (!allQuestionsToUpdate.isEmpty()) {
            questionRepository.saveAll(allQuestionsToUpdate);
        }
        if (!allSessionsToUpdate.isEmpty()) {
            sessionRepository.saveAll(allSessionsToUpdate);
        }
        if (!rewardsToBatch.isEmpty()) {
            counterMetricsService.awardPointsAndApprovedQuestionsBatch(rewardsToBatch);
        }

        log.info("Automated AI review fallback workflow completed successfully.");
    }
}
