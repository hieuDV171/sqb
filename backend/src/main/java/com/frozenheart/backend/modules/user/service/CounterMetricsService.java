package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import java.util.List;

public interface CounterMetricsService {

    // --- UserProfile Counters & Points ---
    void incrementProposedQuestions(Long userId, int delta);

    void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta);

    void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId);

    void awardPointsAndApprovedQuestions(Long userId, double pointsDelta, int approvedDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId, Subject subject);

    void awardPointsAndApprovedQuestionsBatch(List<UserPointRewardDto> rewards);

    void awardPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId);

    void awardPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId, Subject subject);

    void awardSecretPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId);

    void awardSecretPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId, Subject subject);

    void deductPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId);

    void deductPublicPoints(Long userId, double pointsDelta, PointHistoryReason reason, PointHistoryTargetType targetType, Long targetId, Subject subject);

    void finalizeSemesterPoints();

    void incrementBadgesCount(Long userId, int delta);

    void incrementFriendsCount(Long userId, int delta);

    void updateFollowerRelationCounts(Long followerId, Long followedUserId, int delta);

    // --- Session Counters ---
    void incrementSessionReacts(Long sessionId, int delta);

    void incrementSessionComments(Long sessionId, int delta);

    // --- Post Counters ---
    void incrementPostReacts(Long postId, int delta);

    void incrementPostComments(Long postId, int delta);

    // --- Question Counters ---
    void incrementQuestionReacts(Long questionId, int delta);

    void incrementQuestionComments(Long questionId, int delta);

    void updateQuestionRatingStats(Long questionId, double avgRating, int ratingCount);

    // --- Reconciliation (Sync Job) ---
    void reconcileAllUserProfileCounters();

}
