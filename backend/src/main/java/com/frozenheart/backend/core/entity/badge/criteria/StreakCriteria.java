package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
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
@JsonTypeName("STREAK")
@Schema(description = "Tiêu chí chuỗi học liên tiếp, ghép với trigger_event STUDY_STREAK")
public class StreakCriteria implements BadgeCriteria {

    @Schema(description = "Số ngày liên tiếp cần đạt, >=1", example = "7", minimum = "1")
    private int streakDays;

    @Override
    @Schema(description = "Loại tiêu chí cố định", example = "STREAK")
    public CriteriaType getType() {
        return CriteriaType.STREAK;
    }
}
