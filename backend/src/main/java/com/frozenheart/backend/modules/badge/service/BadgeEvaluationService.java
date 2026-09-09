package com.frozenheart.backend.modules.badge.service;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.badge.evaluator.BadgeEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BadgeEvaluationService {

    private final Map<CriteriaType, BadgeEvaluator<? extends BadgeCriteria>> evaluatorMap;

    public BadgeEvaluationService(List<BadgeEvaluator<? extends BadgeCriteria>> evaluators) {
        this.evaluatorMap = evaluators.stream()
                .collect(Collectors.toMap(
                        BadgeEvaluator::getSupportedType,
                        e -> e
                ));
        log.info("[BadgeEvaluationService] Đã nạp {} bộ đánh giá tiêu chí huy hiệu: {}", evaluatorMap.size(), evaluatorMap.keySet());
    }

    public boolean isEligible(User user, Badge badge, Object context) {
        if (user == null || badge == null || badge.getCriteria() == null) {
            return false;
        }

        BadgeCriteria criteria = badge.getCriteria();
        CriteriaType type = criteria.getType();

        BadgeEvaluator<? extends BadgeCriteria> evaluator = evaluatorMap.get(type);
        if (evaluator == null) {
            log.warn("[BadgeEvaluationService] Không tìm thấy bộ đánh giá cho tiêu chí: {}", type);
            return false;
        }

        try {
            return evaluate(evaluator, user, badge, criteria, context);
        } catch (Exception e) {
            log.error("[BadgeEvaluationService] Lỗi khi đánh giá huy hiệu {} (ID: {}) cho user {}: {}",
                    badge.getName(), badge.getId(), user.getId(), e.getMessage());
            return false;
        }
    }

    private <T extends BadgeCriteria> boolean evaluate(
            BadgeEvaluator<T> evaluator, User user, Badge badge, BadgeCriteria criteria, Object context) {
        return evaluator.evaluate(user, badge, (T) criteria, context);
    }
}
