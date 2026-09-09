package com.frozenheart.backend.modules.user.service.impl;

import com.frozenheart.backend.core.dto.event.PointAddedEvent;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;
import com.frozenheart.backend.core.entity.prediction.PointHistory;
import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import com.frozenheart.backend.modules.gamification.repository.CoinTransactionRepository;
import com.frozenheart.backend.modules.gamification.repository.PointHistoryRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterMetricsServiceImpl implements CounterMetricsService {

    private final UserProfileRepository userProfileRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final PostRepository postRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final CurrentSemesterHolder currentSemesterHolder;
    private final BadgeService badgeService;

    private final ApplicationEventPublisher eventPublisher;

    // ==========================================
    // 1. CHỈ SỐ USER PROFILE (Gamification & Social)
    // ==========================================

    @Override
    @Transactional
    public void incrementProposedQuestions(Long userId, int delta) {
        if (userId == null || delta == 0)
            return;
        userProfileRepository.incrementProposedQuestions(userId, delta);
        if (delta > 0) {
            User userRef = userRepository.getReferenceById(userId);
            badgeService.checkAndGrantBadges(userRef, BadgeTriggerEvent.PROPOSED_QUESTIONS, null);
        }
    }


    @Override
    @Transactional
    public void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta) {
        awardPointsAndApprovedQuestions(userId, pointsDelta, approvedDelta, PointHistoryReason.APPROVED_QUESTION_REWARD, PointHistoryTargetType.QUESTION, null, null);
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta, PointHistoryReason reason,
            PointHistoryTargetType targetType, Long targetId) {
        awardPointsAndApprovedQuestions(userId, pointsDelta, approvedDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta, PointHistoryReason reason,
            PointHistoryTargetType targetType, Long targetId, Subject subject) {
        if (userId == null)
            return;

        UserPointRewardDto reward = new UserPointRewardDto(userId, pointsDelta, approvedDelta, reason, targetType,
                targetId, subject);
        awardPointsAndApprovedQuestionsBatch(List.of(reward));
    }

    @Override
    @Transactional
    public void awardPointsAndApprovedQuestionsBatch(List<UserPointRewardDto> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        Set<Long> userIds = rewards.stream()
                .map(UserPointRewardDto::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty())
            return;

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Map<Long, UserGamification> gamificationMap = userGamificationRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserGamification::getUserId, g -> g));

        List<UserProfile> profilesToSave = new ArrayList<>();
        Map<Long, UserGamification> gamificationsToSave = new HashMap<>();
        List<PointHistory> historiesToSave = new ArrayList<>();
        List<CoinTransaction> coinTransactionsToSave = new ArrayList<>();
        Semester currentSemester = currentSemesterHolder.getCurrentSemester();

        for (UserPointRewardDto reward : rewards) {
            if (reward.userId() == null)
                continue;

            UserProfile profile = profileMap.get(reward.userId());
            if (profile != null && reward.approvedDelta() != 0) {
                profile.setTotalApprovedQuestions(profile.getTotalApprovedQuestions() + reward.approvedDelta());
                profilesToSave.add(profile);
            }

            if (reward.pointsDelta() > 0.0) {
                UserGamification g = gamificationsToSave.computeIfAbsent(reward.userId(), id ->
                        gamificationMap.computeIfAbsent(id, uid -> UserGamification.builder()
                                .user(userRepository.getReferenceById(uid))
                                .build())
                );
                g.setPublicPoints(g.getPublicPoints() + reward.pointsDelta());
                g.setCoinBalance(g.getCoinBalance() + reward.pointsDelta());

                User userRef = userRepository.getReferenceById(reward.userId());

                PointHistoryReason reason = reward.reason() != null ? reward.reason() : PointHistoryReason.APPROVED_QUESTION_REWARD;

                PointHistory history = PointHistory.builder()
                        .user(userRef)
                        .points(reward.pointsDelta())
                        .reason(reason)
                        .targetType(reward.targetType())
                        .targetId(reward.targetId())
                        .subject(reward.subject())
                        .semester(currentSemester)
                        .createdAt(Instant.now())
                        .build();
                historiesToSave.add(history);

                // Ghi sổ cái bất biến (CoinTransaction - Ledger)
                CoinTransaction coinTx = CoinTransaction.builder()
                        .user(userRef)
                        .amount(reward.pointsDelta())
                        .balanceAfter(g.getCoinBalance())
                        .type(CoinTransactionType.QUESTION_APPROVED)
                        .description("Thưởng duyệt câu hỏi: +" + reward.pointsDelta() + " xu (" + reason.name() + ")")
                        .targetType(mapToCoinTargetType(reward.targetType()))
                        .targetId(reward.targetId())
                        .createdAt(Instant.now())
                        .build();
                coinTransactionsToSave.add(coinTx);

                eventPublisher.publishEvent(PointAddedEvent.builder()
                        .userId(reward.userId())
                        .points(reward.pointsDelta())
                        .subjectId(reward.subject() != null ? reward.subject().getId() : null)
                        .build());
            }
        }

        if (!profilesToSave.isEmpty()) {
            userProfileRepository.saveAll(profilesToSave);
        }
        if (!gamificationsToSave.isEmpty()) {
            userGamificationRepository.saveAll(gamificationsToSave.values());
        }
        if (!historiesToSave.isEmpty()) {
            pointHistoryRepository.saveAll(historiesToSave);
        }
        if (!coinTransactionsToSave.isEmpty()) {
            coinTransactionRepository.saveAll(coinTransactionsToSave);
        }

        // Tự động kiểm tra và trao huy hiệu theo lô (ngăn ngừa N+1)
        Set<Long> approvedUserIds = rewards.stream()
                .filter(r -> r.userId() != null && r.approvedDelta() > 0)
                .map(UserPointRewardDto::userId)
                .collect(Collectors.toSet());
        if (!approvedUserIds.isEmpty()) {
            List<User> usersWithApproved = approvedUserIds.stream()
                    .map(userRepository::getReferenceById)
                    .toList();
            badgeService.checkAndGrantBadgesBatch(usersWithApproved, BadgeTriggerEvent.APPROVED_QUESTIONS, null);
        }

        Set<Long> pointsUserIds = rewards.stream()
                .filter(r -> r.userId() != null && r.pointsDelta() > 0.0)
                .map(UserPointRewardDto::userId)
                .collect(Collectors.toSet());
        if (!pointsUserIds.isEmpty()) {
            List<User> usersWithPoints = pointsUserIds.stream()
                    .map(userRepository::getReferenceById)
                    .toList();
            badgeService.checkAndGrantBadgesBatch(usersWithPoints, BadgeTriggerEvent.PUBLIC_POINTS, null);
        }
    }

    @Override
    @Transactional
    public void awardPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId) {
        awardPublicPoints(userId, pointsDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void awardPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId,
            Subject subject) {
        if (userId == null || pointsDelta <= 0) {
            return;
        }

        PointHistoryReason finalReason = reason != null ? reason : PointHistoryReason.PUBLIC_POINTS_AWARDED;

        userGamificationRepository.findById(userId).ifPresentOrElse(g -> {
            g.setPublicPoints(g.getPublicPoints() + pointsDelta);
            g.setCoinBalance(g.getCoinBalance() + pointsDelta);
            userGamificationRepository.save(g);

            recordCoinTx(userId, pointsDelta, g.getCoinBalance(), CoinTransactionType.GAME_REWARD,
                    "Cộng điểm thưởng công khai: +" + pointsDelta + " xu (" + finalReason.name() + ")",
                    mapToCoinTargetType(targetType), targetId);
        }, () -> {
            User userRef = userRepository.getReferenceById(userId);
            UserGamification g = UserGamification.builder()
                    .user(userRef)
                    .publicPoints(pointsDelta)
                    .coinBalance(pointsDelta)
                    .build();
            userGamificationRepository.save(g);

            recordCoinTx(userId, pointsDelta, pointsDelta, CoinTransactionType.GAME_REWARD,
                    "Cộng điểm thưởng công khai: +" + pointsDelta + " xu (" + finalReason.name() + ")",
                    mapToCoinTargetType(targetType), targetId);
        });

        User userRef = userRepository.getReferenceById(userId);
        Semester currentSemester = currentSemesterHolder.getCurrentSemester();

        PointHistory history = PointHistory.builder()
                .user(userRef)
                .points(pointsDelta)
                .reason(finalReason)
                .targetType(targetType)
                .targetId(targetId)
                .subject(subject)
                .semester(currentSemester)
                .createdAt(Instant.now())
                .build();
        pointHistoryRepository.save(history);

        eventPublisher.publishEvent(PointAddedEvent.builder()
                .userId(userId)
                .points(pointsDelta)
                .subjectId(subject != null ? subject.getId() : null)
                .build());

        // Tự động kiểm tra huy hiệu Public Points
        badgeService.checkAndGrantBadges(userRef, BadgeTriggerEvent.PUBLIC_POINTS, null);
    }

    @Override
    @Transactional
    public void awardSecretPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId) {
        awardSecretPoints(userId, pointsDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void awardSecretPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId,
            Subject subject) {
        if (userId == null || pointsDelta <= 0)
            return;

        PointHistoryReason finalReason = reason != null ? reason : PointHistoryReason.SECRET_POINTS_AWARDED;

        userGamificationRepository.findById(userId).ifPresentOrElse(g -> {
            g.setSecretPoints(g.getSecretPoints() + pointsDelta);
            userGamificationRepository.save(g);
        }, () -> {
            User userRef = userRepository.getReferenceById(userId);
            UserGamification g = UserGamification.builder()
                    .user(userRef)
                    .secretPoints(pointsDelta)
                    .build();
            userGamificationRepository.save(g);
        });

        User userRef = userRepository.getReferenceById(userId);
        Semester currentSemester = currentSemesterHolder.getCurrentSemester();
        PointHistory history = PointHistory.builder()
                .user(userRef)
                .points(pointsDelta)
                .reason(finalReason)
                .targetType(targetType)
                .targetId(targetId)
                .subject(subject)
                .semester(currentSemester)
                .createdAt(Instant.now())
                .build();
        pointHistoryRepository.save(history);

        // KHÔNG publish PointAddedEvent ở đây để bảo mật tuyệt đối điểm bí mật trên Redis Leaderboard trong suốt học kỳ
    }

    @Override
    @Transactional
    public void deductPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId) {
        deductPublicPoints(userId, pointsDelta, reason, targetType, targetId, null);
    }

    @Override
    @Transactional
    public void deductPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId,
            Subject subject) {
        if (userId == null || pointsDelta <= 0)
            return;

        PointHistoryReason finalReason = reason != null ? reason : PointHistoryReason.PUBLIC_POINTS_DEDUCTED;

        userGamificationRepository.findById(userId).ifPresent(g -> {
            g.setPublicPoints(Math.max(0.0, g.getPublicPoints() - pointsDelta));
            g.setCoinBalance(Math.max(0.0, g.getCoinBalance() - pointsDelta));
            userGamificationRepository.save(g);

            recordCoinTx(userId, -pointsDelta, g.getCoinBalance(), CoinTransactionType.PENALTY_DEDUCTION,
                    "Phạt trừ điểm: -" + pointsDelta + " xu (" + finalReason.name() + ")",
                    mapToCoinTargetType(targetType), targetId);

            User userRef = userRepository.getReferenceById(userId);
            Semester currentSemester = currentSemesterHolder.getCurrentSemester();
            PointHistory history = PointHistory.builder()
                    .user(userRef)
                    .points(-pointsDelta)
                    .reason(finalReason)
                    .targetType(targetType)
                    .targetId(targetId)
                    .subject(subject)
                    .semester(currentSemester)
                    .createdAt(Instant.now())
                    .build();
            pointHistoryRepository.save(history);

            eventPublisher.publishEvent(
                    PointAddedEvent.builder()
                            .userId(userId)
                            .points(-pointsDelta)
                            .subjectId(subject != null ? subject.getId() : null)
                            .build());
        });
    }

    @Override
    @Transactional
    public void finalizeSemesterPoints() {
        List<UserGamification> gamifications = userGamificationRepository.findAll();
        List<UserGamification> toSave = new ArrayList<>();
        List<User> usersToGrantBadges = new ArrayList<>();
        List<CoinTransaction> coinTransactionsToSave = new ArrayList<>();
        Semester currentSemester = currentSemesterHolder.getCurrentSemester();

        for (UserGamification g : gamifications) {
            if (g.getSecretPoints() > 0) {
                double currentPublic = g.getPublicPoints();
                double currentSecret = g.getSecretPoints();

                g.setPublicPoints(currentPublic + currentSecret);
                g.setCoinBalance(g.getCoinBalance() + currentSecret);
                g.setSecretPoints(0.0);
                toSave.add(g);

                User userRef = userRepository.getReferenceById(g.getUserId());
                usersToGrantBadges.add(userRef);

                CoinTransaction coinTx = CoinTransaction.builder()
                        .user(userRef)
                        .amount(currentSecret)
                        .balanceAfter(g.getCoinBalance())
                        .type(CoinTransactionType.GAME_REWARD)
                        .description("Kết chuyển điểm bí mật cuối kỳ sang xu")
                        .targetType(CoinTransactionTargetType.SEMESTER)
                        .targetId(currentSemester != null ? currentSemester.getId() : null)
                        .createdAt(Instant.now())
                        .build();
                coinTransactionsToSave.add(coinTx);

                // Cuối kỳ: Điểm bí mật chính thức được công bố lên Redis Leaderboard
                eventPublisher.publishEvent(
                        PointAddedEvent.builder()
                                .userId(g.getUserId())
                                .points(currentSecret)
                                .subjectId(null)
                                .build());
            }
        }

        if (!toSave.isEmpty()) {
            userGamificationRepository.saveAll(toSave);
        }
        if (!coinTransactionsToSave.isEmpty()) {
            coinTransactionRepository.saveAll(coinTransactionsToSave);
        }

        // Tự động kiểm tra và trao huy hiệu PUBLIC_POINTS theo lô (ngăn ngừa N+1)
        if (!usersToGrantBadges.isEmpty()) {
            badgeService.checkAndGrantBadgesBatch(usersToGrantBadges, BadgeTriggerEvent.PUBLIC_POINTS, null);
        }
    }

    private void recordCoinTx(Long userId, double amount, double balanceAfter, CoinTransactionType type,
                              String description, CoinTransactionTargetType targetType, Long targetId) {
        User userRef = userRepository.getReferenceById(userId);
        CoinTransaction tx = CoinTransaction.builder()
                .user(userRef)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .type(type)
                .description(description)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(Instant.now())
                .build();
        coinTransactionRepository.save(tx);
    }

    private CoinTransactionTargetType mapToCoinTargetType(PointHistoryTargetType targetType) {
        if (targetType == null) return CoinTransactionTargetType.SYSTEM;
        return switch (targetType) {
            case QUESTION -> CoinTransactionTargetType.QUESTION;
            case SESSION -> CoinTransactionTargetType.SESSION;
            case COURSE_CLASS -> CoinTransactionTargetType.COURSE_CLASS;
            case SEMESTER -> CoinTransactionTargetType.SEMESTER;
            default -> CoinTransactionTargetType.SYSTEM;
        };
    }

    @Override
    @Transactional
    public void incrementBadgesCount(Long userId, int delta) {
        if (userId == null || delta == 0)
            return;
        userProfileRepository.incrementBadgesCount(userId, delta);
    }

    @Override
    @Transactional
    public void incrementFriendsCount(Long userId, int delta) {
        if (userId == null || delta == 0)
            return;
        userProfileRepository.incrementFriendsCount(userId, delta);
    }

    @Override
    @Transactional
    public void updateFollowerRelationCounts(Long followerId, Long followedUserId, int delta) {
        if (followerId != null && delta != 0) {
            userProfileRepository.incrementFollowingCount(followerId, delta);
        }
        if (followedUserId != null && delta != 0) {
            userProfileRepository.incrementFollowersCount(followedUserId, delta);
        }
    }

    // ==========================================
    // 2. CHỈ SỐ TƯƠNG TÁC XÃ HỘI (Sessions, Questions, Posts)
    // ==========================================

    @Override
    @Transactional
    public void incrementSessionReacts(Long sessionId, int delta) {
        if (sessionId == null || delta == 0)
            return;

        // TODO [TẦNG 3 - REDIS HIGH CONCURRENCY]: Nếu phiên quá hot (> 100 req/s), tìm
        // hiểu cách để ghi Redis

        sessionRepository.incrementReactCount(sessionId, delta);
    }

    @Override
    @Transactional
    public void incrementSessionComments(Long sessionId, int delta) {
        if (sessionId == null || delta == 0)
            return;
        sessionRepository.incrementCommentCount(sessionId, delta);
    }

    // ==========================================
    // 3. CHỈ SỐ CÂU HỎI (QUESTION)
    // ==========================================

    @Override
    @Transactional
    public void incrementQuestionReacts(Long questionId, int delta) {
        if (questionId == null || delta == 0)
            return;
        questionRepository.incrementReactCount(questionId, delta);
    }

    @Override
    @Transactional
    public void incrementQuestionComments(Long questionId, int delta) {
        if (questionId == null || delta == 0)
            return;
        questionRepository.incrementCommentCount(questionId, delta);
    }

    @Override
    @Transactional
    public void updateQuestionRatingStats(Long questionId, double avgRating, int ratingCount) {
        if (questionId == null)
            return;
        questionRepository.updateRatingStats(questionId, avgRating, ratingCount);
    }

    // ==========================================
    // 4. CHỈ SỐ BÀI VIẾT (POST)
    // ==========================================

    @Override
    @Transactional
    public void incrementPostReacts(Long postId, int delta) {
        if (postId == null || delta == 0)
            return;

        // TODO [TẦNG 3 - REDIS HIGH CONCURRENCY]

        postRepository.incrementReactCount(postId, delta);
    }

    @Override
    @Transactional
    public void incrementPostComments(Long postId, int delta) {
        if (postId == null || delta == 0)
            return;
        postRepository.incrementCommentCount(postId, delta);
    }

    // ==========================================
    // 5. TẦNG 4: CRONJOB ĐỒNG BỘ ĐỊNH KỲ (DATA RECONCILIATION)
    // ==========================================

    @Override
    @Transactional
    public void reconcileAllUserProfileCounters() {
        log.info("Starting background metrics reconciliation job...");

        // TODO [TẦNG 4 - RECONCILIATION SQL]

        log.info("Background metrics reconciliation job completed successfully.");
    }
}
