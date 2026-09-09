package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.BadgeTargetRole;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COUNTER_THRESHOLD")
public class CounterThresholdCriteria implements BadgeCriteria {

    private int threshold;

    @Builder.Default
    private BadgeTargetRole targetRole = BadgeTargetRole.ALL;

    @Override
    public CriteriaType getType() {
        return CriteriaType.COUNTER_THRESHOLD;
    }
}
