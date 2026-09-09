package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
public class LeaderboardRankCriteria implements BadgeCriteria {

    private int targetRank; // e.g. 1 for Top 1, 3 for Top 3, 10 for Top 10
    private LeaderboardPeriod period;   // e.g. SEMESTER, SUBJECT

    @Override
    public CriteriaType getType() {
        return CriteriaType.LEADERBOARD_RANK;
    }
}
