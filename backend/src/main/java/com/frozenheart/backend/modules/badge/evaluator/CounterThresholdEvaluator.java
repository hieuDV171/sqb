package com.frozenheart.backend.modules.badge.evaluator;

import com.frozenheart.backend.core.entity.badge.*;
import com.frozenheart.backend.core.entity.badge.criteria.CounterThresholdCriteria;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CounterThresholdEvaluator implements BadgeEvaluator<CounterThresholdCriteria> {

    private final UserProfileRepository userProfileRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final FriendshipRepository friendshipRepository;
    private final SessionRepository sessionRepository;

    @Override
    public CriteriaType getSupportedType() {
        return CriteriaType.COUNTER_THRESHOLD;
    }

    @Override
    public boolean evaluate(User user, Badge badge, CounterThresholdCriteria criteria, Object context) {
        if (user == null || badge == null || criteria == null) {
            return false;
        }

        // Role check
        if (criteria.getTargetRole() != null && criteria.getTargetRole() != BadgeTargetRole.ALL) {
            if (user.getRole() == null || !criteria.getTargetRole().name().equalsIgnoreCase(user.getRole().name())) {
                return false;
            }
        }

        BadgeTriggerEvent metric = badge.getBadgeTriggerEvent();
        int threshold = criteria.getThreshold();
        if (metric == null || threshold <= 0) {
            return false;
        }

        return switch (metric) {
            case APPROVED_QUESTIONS -> userProfileRepository.findById(user.getId())
                    .map(profile -> profile.getTotalApprovedQuestions() >= threshold).orElse(false);
            case PROPOSED_QUESTIONS -> userProfileRepository.findById(user.getId())
                    .map(profile -> profile.getTotalProposedQuestion() >= threshold).orElse(false);
            case ACCEPTED_FRIENDS -> friendshipRepository.countTotalFriends(user.getId()) >= threshold;
            case PUBLIC_POINTS -> userGamificationRepository.findById(user.getId())
                    .map(g -> g.getPublicPoints() >= threshold)
                    .orElse(false);
            case REVIEWED_SESSIONS -> sessionRepository.countByReviewerIdAndStatus(user.getId(), SessionStatus.RESOLVED) >= threshold;
            default -> false;
        };
    }
}
