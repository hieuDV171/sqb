package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsTypingBroadcastDto {

    @Builder.Default
    private ChatWsEventType eventType = ChatWsEventType.TYPING_STATUS;

    private Long conversationId;

    private UserSummaryDto user;

    private boolean isTyping;

    private Instant timestamp;
}
