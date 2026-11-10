package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;

import java.time.Instant;

public record WsTypingBroadcastDto(
        ChatWsEventType eventType,
        Long conversationId,
        UserSummaryDto user,
        boolean typing,
        Instant timestamp) {
}
