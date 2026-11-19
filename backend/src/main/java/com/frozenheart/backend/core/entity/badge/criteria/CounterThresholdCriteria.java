package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Xét ngưỡng bộ đếm; dùng với sự kiện số câu duyệt/đề xuất, phiên đã duyệt, bạn bè hoặc điểm công khai")
public class CounterThresholdCriteria implements BadgeCriteria {

    @Schema(description = "Ngưỡng cần đạt, phải >=1", example = "10", minimum = "1")
    private int threshold;

    @Builder.Default
    @Schema(description = "Phạm vi vai trò được xét, mặc định ALL", example = "ALL")
    private BadgeTargetRole targetRole = BadgeTargetRole.ALL;

    @Override
    @Schema(description = "Loại tiêu chí cố định", example = "COUNTER_THRESHOLD")
    public CriteriaType getType() {
        return CriteriaType.COUNTER_THRESHOLD;
    }
}
