package com.frozenheart.backend.modules.badge.evaluator;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.core.entity.badge.criteria.StreakCriteria;
import com.frozenheart.backend.core.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class StreakEvaluator implements BadgeEvaluator<StreakCriteria> {

    @Override
    public CriteriaType getSupportedType() {
        return CriteriaType.STREAK;
    }

    @Override
    public boolean evaluate(User user, Badge badge, StreakCriteria criteria, Object context) {
        if (user == null || criteria == null) {
            return false;
        }
        if (context instanceof Number streakDays) {
            return streakDays.intValue() >= criteria.getStreakDays();
        }
        return false;
    }
}
