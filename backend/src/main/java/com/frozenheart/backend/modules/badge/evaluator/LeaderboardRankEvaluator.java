package com.frozenheart.backend.modules.badge.evaluator;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.core.entity.badge.criteria.LeaderboardRankCriteria;
import com.frozenheart.backend.core.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardRankEvaluator implements BadgeEvaluator<LeaderboardRankCriteria> {

    @Override
    public CriteriaType getSupportedType() {
        return CriteriaType.LEADERBOARD_RANK;
    }

    @Override
    public boolean evaluate(User user, Badge badge, LeaderboardRankCriteria criteria, Object context) {
        if (user == null || criteria == null) {
            return false;
        }

        if (context instanceof Number rank) {
            return rank.intValue() > 0 && rank.intValue() <= criteria.getTargetRank();
        }
        return false;
    }
}
