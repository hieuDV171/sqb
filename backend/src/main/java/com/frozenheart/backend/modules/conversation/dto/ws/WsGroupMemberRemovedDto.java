package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;

public record WsGroupMemberRemovedDto(ChatWsEventType eventType, Long conversationId, Long removedUserId) {
    public WsGroupMemberRemovedDto(Long conversationId, Long removedUserId) {
        this(ChatWsEventType.GROUP_MEMBER_REMOVED, conversationId, removedUserId);
    }
}
