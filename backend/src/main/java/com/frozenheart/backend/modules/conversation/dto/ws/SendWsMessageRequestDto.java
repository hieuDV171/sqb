package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.core.entity.conversation.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendWsMessageRequestDto {

    @NotNull(message = "conversation_id không được để trống")
    private Long conversationId;

    private String content;

    private MessageType messageType;

    private List<String> mediaUrls;

    private Long replyToMessageId;
}
