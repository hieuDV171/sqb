import {
  useQuery,
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import { chatService } from '../services/chatService';
import type {
  CreateDirectConversationDto,
  CreateGroupConversationDto,
  UpdateGroupInfoDto,
  AddGroupMembersRequestDto,
  UpdateMemberRoleRequestDto,
  LeaveGroupRequestDto,
  SetHiddenChatPinDto,
  UnlockHiddenChatDto,
  DeleteMessageRequestDto,
} from '../types/chat.types';
import { toast } from '@/stores/useToastStore';

export const CHAT_QUERY_KEYS = {
  conversations: ['conversations'] as const,
  hiddenConversations: ['conversations', 'hidden'] as const,
  groupMembers: (conversationId: number) => ['conversations', conversationId, 'members'] as const,
  messages: (conversationId: number) => ['messages', conversationId] as const,
};

/**
 * Hook lấy danh sách cuộc trò chuyện phân trang con trỏ
 */
export function useConversations(limit: number = 20) {
  return useInfiniteQuery({
    queryKey: CHAT_QUERY_KEYS.conversations,
    queryFn: async ({ pageParam }) => {
      const res = await chatService.getConversations(pageParam as number | undefined, limit);
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      return lastPage.pagination.hasNext && lastPage.pagination.after
        ? lastPage.pagination.after
        : undefined;
    },
  });
}

/**
 * Hook lấy danh sách cuộc trò chuyện trong kho ẩn (chỉ chạy khi đã mở khóa PIN)
 */
export function useHiddenConversations(enabled: boolean = false, limit: number = 20) {
  return useInfiniteQuery({
    queryKey: CHAT_QUERY_KEYS.hiddenConversations,
    queryFn: async ({ pageParam }) => {
      const res = await chatService.getHiddenConversations(pageParam as number | undefined, limit);
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      return lastPage.pagination.hasNext && lastPage.pagination.after
        ? lastPage.pagination.after
        : undefined;
    },
    enabled,
  });
}

/**
 * Hook lấy danh sách thành viên nhóm
 */
export function useGroupMembers(conversationId: number | null, enabled: boolean = true) {
  return useQuery({
    queryKey: conversationId ? CHAT_QUERY_KEYS.groupMembers(conversationId) : ['conversations', 'null', 'members'],
    queryFn: async () => {
      if (!conversationId) return [];
      const res = await chatService.getGroupMembers(conversationId);
      return res.data;
    },
    enabled: enabled && !!conversationId,
  });
}

/**
 * Hook lấy lịch sử tin nhắn trong cuộc hội thoại (cuộn ngược vô tận)
 */
export function useMessages(conversationId: number | null, limit: number = 30) {
  return useInfiniteQuery({
    queryKey: conversationId ? CHAT_QUERY_KEYS.messages(conversationId) : ['messages', 'null'],
    queryFn: async ({ pageParam }) => {
      if (!conversationId) {
        return { conversationId: 0, conversationType: 'DIRECT' as const, messages: [], pagination: {} };
      }
      const res = await chatService.getMessages(conversationId, pageParam as number | undefined, limit);
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      return lastPage.pagination.hasNext && lastPage.pagination.after
        ? lastPage.pagination.after
        : undefined;
    },
    enabled: !!conversationId,
  });
}

/**
 * Mutation tạo chat 1-1
 */
export function useCreateDirectConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateDirectConversationDto) => chatService.createDirectConversation(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
    },
  });
}

/**
 * Mutation tạo nhóm chat
 */
export function useCreateGroupConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateGroupConversationDto) => chatService.createGroupConversation(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      toast.success('Tạo nhóm trò chuyện thành công!');
    },
  });
}

/**
 * Mutation ẩn hội thoại vào kho bí mật
 */
export function useHideConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (conversationId: number) => chatService.hideConversation(conversationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.hiddenConversations });
      toast.success('Đã chuyển cuộc hội thoại vào kho bí mật');
    },
  });
}

/**
 * Mutation bỏ ẩn hội thoại
 */
export function useUnhideConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (conversationId: number) => chatService.unhideConversation(conversationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.hiddenConversations });
      toast.success('Đã khôi phục cuộc trò chuyện ra danh sách chính');
    },
  });
}

/**
 * Mutation cài đặt / đổi mã PIN kho ẩn
 */
export function useSetHiddenPin() {
  return useMutation({
    mutationFn: (data: SetHiddenChatPinDto) => chatService.setHiddenChatPin(data),
    onSuccess: () => {
      toast.success('Cập nhật mã PIN kho ẩn thành công!');
    },
  });
}

/**
 * Mutation mở khóa kho ẩn bằng mã PIN
 */
export function useUnlockHiddenChat() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UnlockHiddenChatDto) => chatService.unlockHiddenChat(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.hiddenConversations });
    },
  });
}

/**
 * Mutation cập nhật thông tin nhóm (Tên, Avatar)
 */
export function useUpdateGroupInfo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ conversationId, data }: { conversationId: number; data: UpdateGroupInfoDto }) =>
      chatService.updateGroupInfo(conversationId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.groupMembers(variables.conversationId) });
      toast.success('Đã cập nhật thông tin nhóm');
    },
  });
}

/**
 * Mutation thêm thành viên vào nhóm
 */
export function useAddGroupMembers() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ conversationId, data }: { conversationId: number; data: AddGroupMembersRequestDto }) =>
      chatService.addGroupMembers(conversationId, data),
    onSuccess: (res, variables) => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.groupMembers(variables.conversationId) });
      const added = res.data?.addedMembers?.length || 0;
      const failed = res.data?.failedMembers?.length || 0;
      if (failed > 0) {
        toast.warning(`Đã thêm ${added} thành viên. Có ${failed} người không thể thêm.`);
      } else {
        toast.success(`Đã thêm thành công ${added} thành viên vào nhóm`);
      }
    },
  });
}

/**
 * Mutation xóa thành viên khỏi nhóm
 */
export function useRemoveGroupMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ conversationId, userId }: { conversationId: number; userId: number }) =>
      chatService.removeGroupMember(conversationId, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.groupMembers(variables.conversationId) });
      toast.success('Đã xóa thành viên khỏi nhóm');
    },
  });
}

/**
 * Mutation cập nhật vai trò thành viên
 */
export function useUpdateMemberRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      conversationId,
      userId,
      data,
    }: {
      conversationId: number;
      userId: number;
      data: UpdateMemberRoleRequestDto;
    }) => chatService.updateMemberRole(conversationId, userId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.groupMembers(variables.conversationId) });
      toast.success('Cập nhật vai trò thành viên thành công');
    },
  });
}

/**
 * Mutation rời nhóm
 */
export function useLeaveGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ conversationId, data }: { conversationId: number; data?: LeaveGroupRequestDto }) =>
      chatService.leaveGroup(conversationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      toast.success('Đã rời khỏi cuộc trò chuyện nhóm');
    },
  });
}

/**
 * Mutation thu hồi / xóa tin nhắn
 */
export function useDeleteMessage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: {
      conversationId: number;
      messageId: number;
      data: DeleteMessageRequestDto;
    }) => chatService.deleteMessage(variables.messageId, variables.data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.messages(variables.conversationId) });
      queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      toast.success(variables.data.scope === 'EVERYONE' ? 'Đã thu hồi tin nhắn' : 'Đã xóa tin nhắn ở phía bạn');
    },
  });
}
