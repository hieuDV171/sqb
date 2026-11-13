import { axiosClient } from '@/api/axiosClient';
import type { CursorResponse, GlobalResponse } from '@/types/response.types';
import type {
  ConversationListResponseDto,
  ConversationDetailDto,
  CreateDirectConversationDto,
  CreateGroupConversationDto,
  GroupCandidateDto,
  GroupInvitationDto,
  UpdateGroupInfoDto,
  ConversationMemberDto,
  AddGroupMembersRequestDto,
  AddGroupMembersResponseDto,
  UpdateMemberRoleRequestDto,
  LeaveGroupRequestDto,
  LeaveGroupResponseDto,
  SetHiddenChatPinDto,
  UnlockHiddenChatDto,
  MessageListResponseDto,
  DeleteMessageRequestDto,
  DeleteMessageResponseDto,
} from '../types/chat.types';

export const chatService = {
  getGroupCandidates(conversationId: number | undefined, search: string, after?: number) {
    return axiosClient.get<unknown, GlobalResponse<CursorResponse<GroupCandidateDto>>>('/conversations/group/candidates', {
      params: { conversationId, search, after },
    });
  },

  getGroupInvitations(after?: number) {
    return axiosClient.get<unknown, GlobalResponse<CursorResponse<GroupInvitationDto>>>('/conversations/group/invitations', {
      params: { after },
      skipToast: true,
    });
  },

  acceptGroupInvitation(invitationId: number) {
    return axiosClient.post<unknown, GlobalResponse<number>>(`/conversations/group/invitations/${invitationId}/accept`);
  },

  declineGroupInvitation(invitationId: number) {
    return axiosClient.delete<unknown, GlobalResponse<void>>(`/conversations/group/invitations/${invitationId}`);
  },

  /**
   * 10.1 Lấy danh sách cuộc hội thoại đang hoạt động
   * GET /api/v1/conversations
   */
  getConversations: async (
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<ConversationListResponseDto>> => {
    return await axiosClient.get<GlobalResponse<ConversationListResponseDto>, GlobalResponse<ConversationListResponseDto>>('/conversations', {
      params: {
        cursor: typeof cursor === 'string' ? cursor : undefined,
        after: typeof cursor === 'number' ? cursor : undefined,
        limit,
      },
    });
  },

  /**
   * 10.2 Tạo cuộc trò chuyện 1-1 (Direct Chat)
   * POST /api/v1/conversations/direct
   */
  createDirectConversation: async (
    data: CreateDirectConversationDto
  ): Promise<GlobalResponse<ConversationDetailDto>> => {
    return await axiosClient.post<GlobalResponse<ConversationDetailDto>, GlobalResponse<ConversationDetailDto>>('/conversations/direct', data);
  },

  /**
   * 10.3 Tạo cuộc trò chuyện nhóm (Group Chat)
   * POST /api/v1/conversations/group
   */
  createGroupConversation: async (
    data: CreateGroupConversationDto
  ): Promise<GlobalResponse<ConversationDetailDto>> => {
    return await axiosClient.post<GlobalResponse<ConversationDetailDto>, GlobalResponse<ConversationDetailDto>>('/conversations/group', data);
  },

  /**
   * 10.4 Ẩn cuộc hội thoại vào kho bí mật
   * PATCH /api/v1/conversations/{id}/hide
   */
  hideConversation: async (conversationId: number): Promise<GlobalResponse<void>> => {
    return await axiosClient.patch<GlobalResponse<void>, GlobalResponse<void>>(`/conversations/${conversationId}/hide`);
  },

  /**
   * 10.5 Bỏ ẩn cuộc hội thoại khỏi kho bí mật
   * PATCH /api/v1/conversations/{id}/unhide
   */
  unhideConversation: async (conversationId: number): Promise<GlobalResponse<void>> => {
    return await axiosClient.patch<GlobalResponse<void>, GlobalResponse<void>>(`/conversations/${conversationId}/unhide`);
  },

  /**
   * 10.6 Cài đặt hoặc đổi mã PIN kho trò chuyện ẩn (6 chữ số)
   * POST /api/v1/users/me/hidden-chat-pin
   */
  setHiddenChatPin: async (data: SetHiddenChatPinDto): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>, GlobalResponse<void>>('/users/me/hidden-chat-pin', data);
  },

  /**
   * 10.7 Mở khóa kho trò chuyện ẩn bằng mã PIN
   * POST /api/v1/conversations/hidden/unlock
   */
  unlockHiddenChat: async (data: UnlockHiddenChatDto) => {
    type Response = GlobalResponse<{ expiresAt: string }>;
    return axiosClient.post<Response, Response>('/conversations/hidden/unlock', data);
  },

  lockHiddenChat: async () => {
    return axiosClient.post<GlobalResponse<void>, GlobalResponse<void>>('/conversations/hidden/lock');
  },

  /**
   * 10.8 Lấy danh sách cuộc trò chuyện trong kho ẩn
   * GET /api/v1/conversations/hidden
   */
  getHiddenConversations: async (
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<ConversationListResponseDto>> => {
    return await axiosClient.get<GlobalResponse<ConversationListResponseDto>, GlobalResponse<ConversationListResponseDto>>('/conversations/hidden', {
      params: {
        cursor: typeof cursor === 'string' ? cursor : undefined,
        after: typeof cursor === 'number' ? cursor : undefined,
        limit,
      },
    });
  },

  /**
   * 10.9 Cập nhật thông tin nhóm trò chuyện (Tên, Avatar)
   * PUT /api/v1/conversations/{id}
   */
  updateGroupInfo: async (
    conversationId: number,
    data: UpdateGroupInfoDto
  ): Promise<GlobalResponse<ConversationDetailDto>> => {
    return await axiosClient.put<GlobalResponse<ConversationDetailDto>, GlobalResponse<ConversationDetailDto>>(`/conversations/${conversationId}`, data);
  },

  /**
   * 10.10 Lấy danh sách thành viên trong nhóm
   * GET /api/v1/conversations/{id}/members
   */
  getGroupMembers: async (
    conversationId: number
  ): Promise<GlobalResponse<ConversationMemberDto[]>> => {
    return await axiosClient.get<GlobalResponse<ConversationMemberDto[]>, GlobalResponse<ConversationMemberDto[]>>(`/conversations/${conversationId}/members`, { skipToast: true });
  },

  /**
   * 10.11 Thêm thành viên vào nhóm chat
   * POST /api/v1/conversations/{id}/members
   */
  addGroupMembers: async (
    conversationId: number,
    data: AddGroupMembersRequestDto
  ): Promise<GlobalResponse<AddGroupMembersResponseDto>> => {
    return await axiosClient.post<GlobalResponse<AddGroupMembersResponseDto>, GlobalResponse<AddGroupMembersResponseDto>>(`/conversations/${conversationId}/members`, data);
  },

  /**
   * 10.12 Xóa thành viên khỏi nhóm chat
   * DELETE /api/v1/conversations/{id}/members/{userId}
   */
  removeGroupMember: async (
    conversationId: number,
    userId: number
  ): Promise<GlobalResponse<void>> => {
    return await axiosClient.delete<GlobalResponse<void>, GlobalResponse<void>>(`/conversations/${conversationId}/members/${userId}`, { skipToast: true });
  },

  /**
   * 10.13 Cập nhật vai trò thành viên nhóm (CHIEF, VILLAGE_ELDER, VILLAGER)
   * PUT /api/v1/conversations/{id}/members/{userId}/role
   */
  updateMemberRole: async (
    conversationId: number,
    userId: number,
    data: UpdateMemberRoleRequestDto
  ): Promise<GlobalResponse<void>> => {
    return await axiosClient.put<GlobalResponse<void>, GlobalResponse<void>>(`/conversations/${conversationId}/members/${userId}/role`, data, { skipToast: true });
  },

  /**
   * 10.14 Rời khỏi nhóm chat (kèm chỉ định Tù trưởng mới nếu là CHIEF)
   * POST /api/v1/conversations/{id}/leave
   */
  leaveGroup: async (
    conversationId: number,
    data?: LeaveGroupRequestDto
  ): Promise<GlobalResponse<LeaveGroupResponseDto>> => {
    return await axiosClient.post<GlobalResponse<LeaveGroupResponseDto>, GlobalResponse<LeaveGroupResponseDto>>(`/conversations/${conversationId}/leave`, data || {});
  },

  /**
   * 11.1 Lấy lịch sử tin nhắn trong cuộc hội thoại (Cursor Pagination)
   * GET /api/v1/conversations/{id}/messages
   */
  getMessages: async (
    conversationId: number,
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<MessageListResponseDto>> => {
    return await axiosClient.get<GlobalResponse<MessageListResponseDto>, GlobalResponse<MessageListResponseDto>>(`/conversations/${conversationId}/messages`, {
      params: { after, limit },
      skipToast: true,
    });
  },

  /**
   * 11.2 Xóa hoặc thu hồi tin nhắn (ME vs EVERYONE)
   * DELETE /api/v1/messages/{id}
   */
  deleteMessage: async (
    messageId: number,
    data: DeleteMessageRequestDto
  ): Promise<GlobalResponse<DeleteMessageResponseDto>> => {
    return await axiosClient.delete<GlobalResponse<DeleteMessageResponseDto>, GlobalResponse<DeleteMessageResponseDto>>(`/messages/${messageId}`, {
      data,
    });
  },
};
