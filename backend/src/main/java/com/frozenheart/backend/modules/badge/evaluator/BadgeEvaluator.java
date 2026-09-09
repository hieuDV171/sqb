package com.frozenheart.backend.modules.badge.evaluator;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.core.entity.user.User;

public interface BadgeEvaluator<T extends BadgeCriteria> {

    CriteriaType getSupportedType();

    boolean evaluate(User user, Badge badge, T criteria, Object context);
}
