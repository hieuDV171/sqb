package com.frozenheart.backend.modules.conversation.dto.ws;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingWsRequestDto {

    @NotNull(message = "conversation_id không được để trống")
    private Long conversationId;

    private boolean isTyping;
}
