package com.frozenheart.backend.modules.socialinteraction.dto;

import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Yêu cầu thả hoặc gỡ cảm xúc")
public class ReactRequest {
    @NotNull(message = "Thiếu trường targetType")
    @Schema(description = "Loại đối tượng (POST, COMMENT, QUESTION, SESSION)", example = "POST", requiredMode = Schema.RequiredMode.REQUIRED)
    private InteractionTargetType targetType;

    @NotNull(message = "Thiếu trường targetId")
    @Schema(description = "ID của đối tượng cần thả cảm xúc", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;

    @NotNull(message = "Thiếu trường reactionType")
    @Schema(description = "Loại cảm xúc tích cực (LIKE, LOVE, WOW)", example = "LIKE", allowableValues = {"LIKE", "LOVE", "WOW"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private ReactionType reactionType;
}
