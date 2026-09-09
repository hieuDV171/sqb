package com.frozenheart.backend.core.entity.badge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.frozenheart.backend.core.entity.badge.criteria.*;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CounterThresholdCriteria.class, name = "COUNTER_THRESHOLD"),
        @JsonSubTypes.Type(value = StreakCriteria.class, name = "STREAK"),
        @JsonSubTypes.Type(value = SubjectMasteryCriteria.class, name = "SUBJECT_MASTERY"),
        @JsonSubTypes.Type(value = LeaderboardRankCriteria.class, name = "LEADERBOARD_RANK"),
        @JsonSubTypes.Type(value = ManualGrantCriteria.class, name = "MANUAL_GRANT")
})
public interface BadgeCriteria extends Serializable {

    CriteriaType getType();

}
