package com.frozenheart.backend.modules.user.service.impl;

import com.frozenheart.backend.core.entity.prediction.PointHistory;
import com.frozenheart.backend.core.entity.user.GamificationPointsJson;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.PointHistoryRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterMetricsServiceImpl implements CounterMetricsService {

    private final UserProfileRepository userProfileRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final PostRepository postRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final CurrentSemesterHolder currentSemesterHolder;

    // ==========================================
    // 1. CHỈ SỐ USER PROFILE (Gamification & Social)
    // ==========================================

    @Override
    @Transactional
    public void incrementProposedQuestions(Long userId, int delta) {
        if (userId == null || delta == 0) return;
        userProfileRepository.incrementProposedQuestions(userId, delta);
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta) {
        awardPointsAndApprovedQuestions(userId, pointsDelta, approvedDelta, "APPROVED_QUESTION_REWARD", "QUESTION", null, null);
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta, String reason, String targetType, Long targetId) {
        awardPointsAndApprovedQuestions(userId, pointsDelta, approvedDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta, String reason, String targetType, Long targetId, Subject subject) {
        if (userId == null || (pointsDelta == 0.0 && approvedDelta == 0)) return;
        if (approvedDelta != 0) {
            userProfileRepository.incrementApprovedQuestions(userId, approvedDelta);
        }
        if (pointsDelta > 0.0) {
            awardPublicPoints(userId, pointsDelta, reason, targetType, targetId, subject);
        }
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestionsBatch(List<UserPointRewardDto> rewards) {
        if (rewards == null || rewards.isEmpty()) return;

        Set<Long> userIds = rewards.stream()
                .map(UserPointRewardDto::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) return;

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<UserProfile> profilesToSave = new ArrayList<>();
        List<PointHistory> historiesToSave = new ArrayList<>();
        Semester currentSemester = currentSemesterHolder.getCurrentSemester();

        for (UserPointRewardDto reward : rewards) {
            if (reward.userId() == null) continue;
            UserProfile profile = profileMap.get(reward.userId());
            if (profile != null) {
                if (reward.approvedDelta() != 0) {
                    profile.setTotalApprovedQuestions(profile.getTotalApprovedQuestions() + reward.approvedDelta());
                }
                if (reward.pointsDelta() > 0.0) {
                    GamificationPointsJson p = profile.getGamificationPoints();
                    if (p == null) p = new GamificationPointsJson(0.0, 0.0);
                    p.setPublicPoints(p.getPublicPoints() + reward.pointsDelta());
                    profile.setGamificationPoints(p);
                }
                profilesToSave.add(profile);

                User userRef = userRepository.getReferenceById(reward.userId());

                PointHistory history = PointHistory.builder()
                        .user(userRef)
                        .points(reward.pointsDelta())
                        .reason(reward.reason() != null ? reward.reason() : "APPROVED_QUESTION_REWARD")
                        .targetType(reward.targetType())
                        .targetId(reward.targetId())
                        .subject(reward.subject())
                        .semester(currentSemester)
                        .createdAt(LocalDateTime.now())
                        .build();
                historiesToSave.add(history);
            }
        }

        if (!profilesToSave.isEmpty()) {
            userProfileRepository.saveAll(profilesToSave);
        }
        if (!historiesToSave.isEmpty()) {
            pointHistoryRepository.saveAll(historiesToSave);
        }
    }

    @Override
    @Transactional
    public void awardPublicPoints(Long userId, double pointsDelta, String reason, String targetType, Long targetId) {
        awardPublicPoints(userId, pointsDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void awardPublicPoints(Long userId, double pointsDelta, String reason, String targetType, Long targetId, Subject subject) {
        if (userId == null || pointsDelta <= 0) {
            return;
        }

        userProfileRepository.findById(userId).ifPresent(profile -> {
            GamificationPointsJson p = profile.getGamificationPoints();
            if (p == null) p = new GamificationPointsJson(0.0, 0.0);
            p.setPublicPoints(p.getPublicPoints() + pointsDelta);
            profile.setGamificationPoints(p);
            userProfileRepository.save(profile);

            User userRef = userRepository.getReferenceById(userId);
            Semester currentSemester = currentSemesterHolder.getCurrentSemester();

            PointHistory history = PointHistory.builder()
                    .user(userRef)
                    .points(pointsDelta)
                    .reason(reason != null ? reason : "PUBLIC_POINTS_AWARDED")
                    .targetType(targetType)
                    .targetId(targetId)
                    .subject(subject)
                    .semester(currentSemester)
                    .createdAt(LocalDateTime.now())
                    .build();
            pointHistoryRepository.save(history);
        });
    }

    @Override
    @Transactional
    public void awardSecretPoints(Long userId, double pointsDelta, String reason, String targetType, Long targetId) {
        awardSecretPoints(userId, pointsDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void awardSecretPoints(Long userId, double pointsDelta, String reason, String targetType, Long targetId, Subject subject) {
        if (userId == null || pointsDelta <= 0) return;

        userProfileRepository.findById(userId).ifPresent(profile -> {
            GamificationPointsJson p = profile.getGamificationPoints();
            if (p == null) p = new GamificationPointsJson(0.0, 0.0);
            p.setSecretPoints(p.getSecretPoints() + pointsDelta);
            profile.setGamificationPoints(p);
            userProfileRepository.save(profile);

            User userRef = userRepository.getReferenceById(userId);
            Semester currentSemester = currentSemesterHolder.getCurrentSemester();
            PointHistory history = PointHistory.builder()
                    .user(userRef)
                    .points(pointsDelta)
                    .reason(reason != null ? reason : "SECRET_POINTS_AWARDED")
                    .targetType(targetType)
                    .targetId(targetId)
                    .subject(subject)
                    .semester(currentSemester)
                    .createdAt(LocalDateTime.now())
                    .build();
            pointHistoryRepository.save(history);
        });
    }

    @Override
    @Transactional
    public void deductPublicPoints(Long userId, double pointsDelta, String reason, String targetType, Long targetId) {
        deductPublicPoints(userId, pointsDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void deductPublicPoints(Long userId, double pointsDelta, String reason, String targetType, Long targetId, Subject subject) {
        if (userId == null || pointsDelta <= 0) return;

        userProfileRepository.findById(userId).ifPresent(profile -> {
            GamificationPointsJson p = profile.getGamificationPoints();
            if (p == null) p = new GamificationPointsJson(0.0, 0.0);
            p.setPublicPoints(Math.max(0.0, p.getPublicPoints() - pointsDelta));
            profile.setGamificationPoints(p);
            userProfileRepository.save(profile);

            User userRef = userRepository.getReferenceById(userId);
            Semester currentSemester = currentSemesterHolder.getCurrentSemester();
            PointHistory history = PointHistory.builder()
                    .user(userRef)
                    .points(-pointsDelta)
                    .reason(reason != null ? reason : "PUBLIC_POINTS_DEDUCTED")
                    .targetType(targetType)
                    .targetId(targetId)
                    .subject(subject)
                    .semester(currentSemester)
                    .createdAt(LocalDateTime.now())
                    .build();
            pointHistoryRepository.save(history);
        });
    }

    @Override
    @Transactional
    public void finalizeSemesterPoints() {
        List<UserProfile> profiles = userProfileRepository.findAll();
        for (UserProfile profile : profiles) {
            GamificationPointsJson points = profile.getGamificationPoints();
            if (points != null && points.getSecretPoints() > 0) {
                double currentPublic = points.getPublicPoints();
                double currentSecret = points.getSecretPoints();

                points.setPublicPoints(currentPublic + currentSecret);
                points.setSecretPoints(0.0);
                profile.setGamificationPoints(points);

                User userRef = userRepository.getReferenceById(profile.getUserId());
                PointHistory history = PointHistory.builder()
                        .user(userRef)
                        .points(currentSecret)
                        .reason("SEMESTER_SECRET_POINTS_CONSOLIDATED")
                        .targetType("SEMESTER")
                        .createdAt(LocalDateTime.now())
                        .build();
                pointHistoryRepository.save(history);
            }
        }
        userProfileRepository.saveAll(profiles);
    }

    @Override
    @Transactional
    public void incrementBadgesCount(Long userId, int delta) {
        if (userId == null || delta == 0) return;
        userProfileRepository.incrementBadgesCount(userId, delta);
    }

    @Override
    @Transactional
    public void incrementFriendsCount(Long userId, int delta) {
        if (userId == null || delta == 0) return;
        userProfileRepository.incrementFriendsCount(userId, delta);
    }

    @Override
    @Transactional
    public void updateFollowerRelationCounts(Long followerId, Long followedId, int delta) {
        if (followerId != null && delta != 0) {
            userProfileRepository.incrementFollowingCount(followerId, delta);
        }
        if (followedId != null && delta != 0) {
            userProfileRepository.incrementFollowersCount(followedId, delta);
        }
    }

    // ==========================================
    // 2. CHỈ SỐ PHIÊN ĐỀ XUẤT (SESSION)
    // ==========================================

    @Override
    @Transactional
    public void incrementSessionReacts(Long sessionId, int delta) {
        if (sessionId == null || delta == 0) return;

        // TODO [TẦNG 3 - REDIS HIGH CONCURRENCY]: Nếu phiên quá hot (> 100 req/s), mở comment bên dưới để ghi Redis:
        // redisTemplate.opsForValue().increment("session:" + sessionId + ":react_delta", delta);

        sessionRepository.incrementReactCount(sessionId, delta);
    }

    @Override
    @Transactional
    public void incrementSessionComments(Long sessionId, int delta) {
        if (sessionId == null || delta == 0) return;
        sessionRepository.incrementCommentCount(sessionId, delta);
    }

    // ==========================================
    // 3. CHỈ SỐ CÂU HỎI (QUESTION)
    // ==========================================

    @Override
    @Transactional
    public void incrementQuestionReacts(Long questionId, int delta) {
        if (questionId == null || delta == 0) return;
        questionRepository.incrementReactCount(questionId, delta);
    }

    @Override
    @Transactional
    public void incrementQuestionComments(Long questionId, int delta) {
        if (questionId == null || delta == 0) return;
        questionRepository.incrementCommentCount(questionId, delta);
    }

    @Override
    @Transactional
    public void updateQuestionRatingStats(Long questionId, double avgRating, int ratingCount) {
        if (questionId == null) return;
        questionRepository.updateRatingStats(questionId, avgRating, ratingCount);
    }

    // ==========================================
    // 4. CHỈ SỐ BÀI VIẾT (POST)
    // ==========================================

    @Override
    @Transactional
    public void incrementPostReacts(Long postId, int delta) {
        if (postId == null || delta == 0) return;

        // TODO [TẦNG 3 - REDIS HIGH CONCURRENCY]:
        // Nếu bài viết VIRAL có hàng ngàn lượt like/giây, mở comment dưới đây để ghi vào Redis thay vì SQL:
        // redisTemplate.opsForValue().increment("post:" + postId + ":react_delta", delta);

        postRepository.incrementReactCount(postId, delta);
    }

    @Override
    @Transactional
    public void incrementPostComments(Long postId, int delta) {
        if (postId == null || delta == 0) return;
        postRepository.incrementCommentCount(postId, delta);
    }

    // ==========================================
    // 5. TẦNG 4: CRONJOB ĐỒNG BỘ ĐỊNH KỲ (DATA RECONCILIATION)
    // ==========================================

    @Override
    @Transactional
    public void reconcileAllUserProfileCounters() {
        log.info("Starting background metrics reconciliation job...");

        // TODO [TẦNG 4 - RECONCILIATION SQL]:
        // Sau này khi hệ thống có > 1,000,000 dòng dữ liệu, mở các câu SQL Native dưới đây để chạy quét ngầm 3:00 AM:

        /*
        // 1. Tính lại totalApprovedQuestions & totalProposedQuestions cho UserProfile:
        userProfileRepository.reconcileTotalApprovedQuestions();
        userProfileRepository.reconcileTotalProposedQuestions();

        // 2. Tính lại reactCount & commentCount cho Session / Post / Question:
        sessionRepository.reconcileSessionReactsAndComments();
        postRepository.reconcilePostReactsAndComments();
        questionRepository.reconcileQuestionStats();
        */

        log.info("Background metrics reconciliation job completed successfully.");
    }
}
