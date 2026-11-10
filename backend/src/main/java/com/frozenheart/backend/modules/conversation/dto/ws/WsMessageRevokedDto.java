package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;

public record WsMessageRevokedDto(ChatWsEventType eventType, Long conversationId, Long messageId) {
    public WsMessageRevokedDto(Long conversationId, Long messageId) {
        this(ChatWsEventType.MESSAGE_REVOKED, conversationId, messageId);
    }
}
