package com.frozenheart.backend.modules.conversation.dto.ws;

import jakarta.validation.constraints.NotNull;

public record TypingWsRequestDto(
        @NotNull(message = "conversation_id không được để trống")
        Long conversationId,
        boolean typing) {
}
