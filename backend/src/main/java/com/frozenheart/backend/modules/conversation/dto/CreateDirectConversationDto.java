package com.frozenheart.backend.modules.conversation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDirectConversationDto {

    @NotNull(message = "target_user_id không được để trống")
    private Long targetUserId;
}
