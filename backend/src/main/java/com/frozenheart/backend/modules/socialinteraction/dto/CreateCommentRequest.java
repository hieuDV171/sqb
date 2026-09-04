package com.frozenheart.backend.modules.socialinteraction.dto;

import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
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
public class CreateCommentRequest {
    @NotNull(message = "Thiếu trường targetType")
    private InteractionTargetType targetType;

    @NotNull(message = "Thiếu trường targetId")
    private Long targetId;

    private String content;

    private Long parentCommentId;

    private String mediaUrl;
}
