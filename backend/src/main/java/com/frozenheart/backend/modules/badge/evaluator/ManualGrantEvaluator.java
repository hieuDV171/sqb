package com.frozenheart.backend.modules.badge.evaluator;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.core.entity.badge.criteria.ManualGrantCriteria;
import com.frozenheart.backend.core.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class ManualGrantEvaluator implements BadgeEvaluator<ManualGrantCriteria> {

    @Override
    public CriteriaType getSupportedType() {
        return CriteriaType.MANUAL_GRANT;
    }

    @Override
    public boolean evaluate(User user, Badge badge, ManualGrantCriteria criteria, Object context) {
        return Boolean.TRUE.equals(context);
    }
}
