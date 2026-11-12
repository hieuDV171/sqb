package com.frozenheart.backend.modules.conversation.constant;

public enum ChatWsEventType {
    NEW_MESSAGE,
    MESSAGE_REVOKED,
    MESSAGE_READ,
    TYPING_STATUS,
    GROUP_ROLES_CHANGED,
    GROUP_MEMBER_REMOVED,
    GROUP_MEMBERS_ADDED
}
