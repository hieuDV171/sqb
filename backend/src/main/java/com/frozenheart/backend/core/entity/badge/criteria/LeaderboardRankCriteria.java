package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
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
@JsonTypeName("LEADERBOARD_RANK")
@Schema(description = "Huy hiệu thứ hạng toàn trường theo học kỳ; ghép với trigger_event LEADERBOARD_RANK")
public class LeaderboardRankCriteria implements BadgeCriteria {

    public static final int MAX_SEMESTER_RANK = 10;

    @Schema(description = "Top cần đạt trong 1..10", example = "3", minimum = "1", maximum = "10")
    private int targetRank; // e.g. 1 for Top 1, 3 for Top 3, 10 for Top 10
    @Schema(description = "Hiện chỉ hỗ trợ SEMESTER", allowableValues = {"SEMESTER"}, example = "SEMESTER")
    private LeaderboardPeriod period;   // e.g. SEMESTER, SUBJECT

    @Override
    @Schema(description = "Loại tiêu chí cố định", example = "LEADERBOARD_RANK")
    public CriteriaType getType() {
        return CriteriaType.LEADERBOARD_RANK;
    }
}
