package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


@Schema(description = "Yêu cầu tái xây dựng lại Bảng xếp hạng trong Redis")
public record RebuildLeaderboardRequest(
        @Schema(description = "Loại phạm vi bảng xếp hạng", example = "SEMESTER")
        @NotNull(message = "MISSING_REQUIRED_PARAMETER")
        LeaderboardPeriod type,

        @Schema(description = "ID môn học (nếu tái tạo bảng xếp hạng theo môn)", example = "10")
        @Positive(message = "INVALID_PARAMETER_VALUE")
        Long subjectId
) {
    @AssertTrue(message = "MISSING_REQUIRED_PARAMETER")
    @Schema(hidden = true)
    public boolean isSubjectSelectionValid() {
        return type != LeaderboardPeriod.SUBJECT || subjectId != null;
    }
}

