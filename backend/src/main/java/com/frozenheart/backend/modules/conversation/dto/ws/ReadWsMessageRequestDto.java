package com.frozenheart.backend.modules.conversation.dto.ws;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadWsMessageRequestDto {

    @NotNull(message = "conversation_id không được để trống")
    private Long conversationId;

    @NotNull(message = "message_id không được để trống")
    private Long messageId;
}
