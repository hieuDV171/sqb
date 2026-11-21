package com.frozenheart.backend.core.entity.badge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.frozenheart.backend.core.entity.badge.criteria.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CounterThresholdCriteria.class, name = "COUNTER_THRESHOLD"),
        @JsonSubTypes.Type(value = StreakCriteria.class, name = "STREAK"),
        @JsonSubTypes.Type(value = SubjectMasteryCriteria.class, name = "SUBJECT_MASTERY"),
        @JsonSubTypes.Type(value = LeaderboardRankCriteria.class, name = "LEADERBOARD_RANK"),
        @JsonSubTypes.Type(value = ManualGrantCriteria.class, name = "MANUAL_GRANT")
})
@Schema(description = "Tiêu chí xét huy hiệu có phân loại bằng type; chọn đúng subtype và trigger_event tương ứng", discriminatorProperty = "type",
        oneOf = {CounterThresholdCriteria.class, StreakCriteria.class, SubjectMasteryCriteria.class, LeaderboardRankCriteria.class, ManualGrantCriteria.class},
        discriminatorMapping = {
                @DiscriminatorMapping(value = "COUNTER_THRESHOLD", schema = CounterThresholdCriteria.class),
                @DiscriminatorMapping(value = "STREAK", schema = StreakCriteria.class),
                @DiscriminatorMapping(value = "SUBJECT_MASTERY", schema = SubjectMasteryCriteria.class),
                @DiscriminatorMapping(value = "LEADERBOARD_RANK", schema = LeaderboardRankCriteria.class),
                @DiscriminatorMapping(value = "MANUAL_GRANT", schema = ManualGrantCriteria.class)
        })
public interface BadgeCriteria extends Serializable {

    @Schema(description = "Discriminator bắt buộc trong JSON, tương ứng với subtype tiêu chí")
    CriteriaType getType();

}
