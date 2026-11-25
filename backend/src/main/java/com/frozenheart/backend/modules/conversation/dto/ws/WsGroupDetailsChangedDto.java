package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;

public record WsGroupDetailsChangedDto(ChatWsEventType eventType, Long conversationId, Long leftUserId) {
    public WsGroupDetailsChangedDto(Long conversationId) {
        this(conversationId, null);
    }

    public WsGroupDetailsChangedDto(Long conversationId, Long leftUserId) {
        this(ChatWsEventType.GROUP_DETAILS_CHANGED, conversationId, leftUserId);
    }
}
