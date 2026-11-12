package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;

public record WsGroupRolesChangedDto(ChatWsEventType eventType, Long conversationId) {
    public WsGroupRolesChangedDto(Long conversationId) {
        this(ChatWsEventType.GROUP_ROLES_CHANGED, conversationId);
    }
}
