import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  ConversationListResponseDto,
  ConversationDetailDto,
  CreateDirectConversationDto,
  CreateGroupConversationDto,
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
  /**
   * 10.1 Lấy danh sách cuộc hội thoại đang hoạt động
   * GET /api/v1/conversations
   */
  getConversations: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<ConversationListResponseDto>> => {
    return await axiosClient.get<GlobalResponse<ConversationListResponseDto>>('/conversations', {
      params: { after, limit },
    }) as any;
  },

  /**
   * 10.2 Tạo cuộc trò chuyện 1-1 (Direct Chat)
   * POST /api/v1/conversations/direct
   */
  createDirectConversation: async (
    data: CreateDirectConversationDto
  ): Promise<GlobalResponse<ConversationDetailDto>> => {
    return await axiosClient.post<GlobalResponse<ConversationDetailDto>>('/conversations/direct', data) as any;
  },

  /**
   * 10.3 Tạo cuộc trò chuyện nhóm (Group Chat)
   * POST /api/v1/conversations/group
   */
  createGroupConversation: async (
    data: CreateGroupConversationDto
  ): Promise<GlobalResponse<ConversationDetailDto>> => {
    return await axiosClient.post<GlobalResponse<ConversationDetailDto>>('/conversations/group', data) as any;
  },

  /**
   * 10.4 Ẩn cuộc hội thoại vào kho bí mật
   * PATCH /api/v1/conversations/{id}/hide
   */
  hideConversation: async (conversationId: number): Promise<GlobalResponse<void>> => {
    return await axiosClient.patch<GlobalResponse<void>>(`/conversations/${conversationId}/hide`) as any;
  },

  /**
   * 10.5 Bỏ ẩn cuộc hội thoại khỏi kho bí mật
   * PATCH /api/v1/conversations/{id}/unhide
   */
  unhideConversation: async (conversationId: number): Promise<GlobalResponse<void>> => {
    return await axiosClient.patch<GlobalResponse<void>>(`/conversations/${conversationId}/unhide`) as any;
  },

  /**
   * 10.6 Cài đặt hoặc đổi mã PIN kho trò chuyện ẩn (6 chữ số)
   * POST /api/v1/users/me/hidden-chat-pin
   */
  setHiddenChatPin: async (data: SetHiddenChatPinDto): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/users/me/hidden-chat-pin', data) as any;
  },

  /**
   * 10.7 Mở khóa kho trò chuyện ẩn bằng mã PIN
   * POST /api/v1/conversations/hidden/unlock
   */
  unlockHiddenChat: async (data: UnlockHiddenChatDto): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/conversations/hidden/unlock', data) as any;
  },

  /**
   * 10.8 Lấy danh sách cuộc trò chuyện trong kho ẩn
   * GET /api/v1/conversations/hidden
   */
  getHiddenConversations: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<ConversationListResponseDto>> => {
    return await axiosClient.get<GlobalResponse<ConversationListResponseDto>>('/conversations/hidden', {
      params: { after, limit },
    }) as any;
  },

  /**
   * 10.9 Cập nhật thông tin nhóm trò chuyện (Tên, Avatar)
   * PUT /api/v1/conversations/{id}
   */
  updateGroupInfo: async (
    conversationId: number,
    data: UpdateGroupInfoDto
  ): Promise<GlobalResponse<ConversationDetailDto>> => {
    return await axiosClient.put<GlobalResponse<ConversationDetailDto>>(`/conversations/${conversationId}`, data) as any;
  },

  /**
   * 10.10 Lấy danh sách thành viên trong nhóm
   * GET /api/v1/conversations/{id}/members
   */
  getGroupMembers: async (
    conversationId: number
  ): Promise<GlobalResponse<ConversationMemberDto[]>> => {
    return await axiosClient.get<GlobalResponse<ConversationMemberDto[]>>(`/conversations/${conversationId}/members`) as any;
  },

  /**
   * 10.11 Thêm thành viên vào nhóm chat
   * POST /api/v1/conversations/{id}/members
   */
  addGroupMembers: async (
    conversationId: number,
    data: AddGroupMembersRequestDto
  ): Promise<GlobalResponse<AddGroupMembersResponseDto>> => {
    return await axiosClient.post<GlobalResponse<AddGroupMembersResponseDto>>(`/conversations/${conversationId}/members`, data) as any;
  },

  /**
   * 10.12 Xóa thành viên khỏi nhóm chat
   * DELETE /api/v1/conversations/{id}/members/{userId}
   */
  removeGroupMember: async (
    conversationId: number,
    userId: number
  ): Promise<GlobalResponse<void>> => {
    return await axiosClient.delete<GlobalResponse<void>>(`/conversations/${conversationId}/members/${userId}`) as any;
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
    return await axiosClient.put<GlobalResponse<void>>(`/conversations/${conversationId}/members/${userId}/role`, data) as any;
  },

  /**
   * 10.14 Rời khỏi nhóm chat (kèm chỉ định Tù trưởng mới nếu là CHIEF)
   * POST /api/v1/conversations/{id}/leave
   */
  leaveGroup: async (
    conversationId: number,
    data?: LeaveGroupRequestDto
  ): Promise<GlobalResponse<LeaveGroupResponseDto>> => {
    return await axiosClient.post<GlobalResponse<LeaveGroupResponseDto>>(`/conversations/${conversationId}/leave`, data || {}) as any;
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
    return await axiosClient.get<GlobalResponse<MessageListResponseDto>>(`/conversations/${conversationId}/messages`, {
      params: { after, limit },
    }) as any;
  },

  /**
   * 11.2 Xóa hoặc thu hồi tin nhắn (ME vs EVERYONE)
   * DELETE /api/v1/messages/{id}
   */
  deleteMessage: async (
    messageId: number,
    data: DeleteMessageRequestDto
  ): Promise<GlobalResponse<DeleteMessageResponseDto>> => {
    return await axiosClient.delete<GlobalResponse<DeleteMessageResponseDto>>(`/messages/${messageId}`, {
      data,
    }) as any;
  },
};
