package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.modules.conversation.dto.*;

import java.util.List;

public interface ConversationService {

    ConversationListResponseDto getConversations(Long after, String cursor, Integer limit);

    ConversationDetailDto createDirectConversation(CreateDirectConversationDto request);

    ConversationDetailDto createGroupConversation(CreateGroupConversationDto request);

    void hideConversation(Long conversationId);

    void unhideConversation(Long conversationId);

    void setHiddenChatPin(SetHiddenChatPinDto request);

    HiddenChatUnlockDto unlockHiddenChat(UnlockHiddenChatDto request);

    void lockHiddenChat();

    ConversationListResponseDto getHiddenConversations(Long after, String cursor, Integer limit);

    // --- Group Management (5.5.3) ---
    ConversationDetailDto updateGroupInfo(Long conversationId, UpdateGroupInfoDto request);

    List<ConversationMemberDto> getGroupMembers(Long conversationId);

    AddGroupMembersResponseDto addGroupMembers(Long conversationId, AddGroupMembersRequestDto request);

    void removeGroupMember(Long conversationId, Long targetUserId);

    void updateMemberRole(Long conversationId, Long targetUserId, UpdateMemberRoleRequestDto request);

    LeaveGroupResponseDto leaveGroup(Long conversationId, LeaveGroupRequestDto request);
}
