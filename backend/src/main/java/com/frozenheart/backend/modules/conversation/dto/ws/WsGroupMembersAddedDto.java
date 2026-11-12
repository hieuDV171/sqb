package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;

public record WsGroupMembersAddedDto(ChatWsEventType eventType, Long conversationId) {
    public WsGroupMembersAddedDto(Long conversationId) {
        this(ChatWsEventType.GROUP_MEMBERS_ADDED, conversationId);
    }
}
