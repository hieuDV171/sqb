import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useStompStatus, useStompSubscription } from '@/lib/stompClient';
import { useAuthStore } from '@/stores/useAuthStore';
import { CHAT_QUERY_KEYS, refreshGroupHistory, refreshGroupMembership, refreshJoinedGroup, resetGroupHistory } from './useChat';
import { useChatStore } from '../stores/useChatStore';
import type {
  WsGroupDetailsChangedDto, WsGroupMemberRemovedDto, WsGroupMembersAddedDto, WsGroupRolesChangedDto,
  WsMessageBroadcastDto, WsMessageRevokedDto, WsReadReceiptBroadcastDto, WsTypingBroadcastDto,
} from '../types/chat.types';

type ChatEvent = WsMessageBroadcastDto | WsMessageRevokedDto | WsReadReceiptBroadcastDto | WsTypingBroadcastDto
  | WsGroupRolesChangedDto | WsGroupMemberRemovedDto | WsGroupMembersAddedDto | WsGroupDetailsChangedDto;

/**
 * Hook lắng nghe tin nhắn và biên nhận đã đọc toàn cục của người dùng
 * Đích đến: /user/queue/messages
 */
export function useChatRealtimeListener() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const queryClient = useQueryClient();
  const stompStatus = useStompStatus();
  const handleIncomingWsMessage = useChatStore((s) => s.handleIncomingWsMessage);
  const handleIncomingReadReceipt = useChatStore((s) => s.handleIncomingReadReceipt);
  const handleIncomingTyping = useChatStore((s) => s.handleIncomingTyping);
  const markMessageRevoked = useChatStore((s) => s.markMessageRevoked);

  useStompSubscription<ChatEvent>(
    '/user/queue/messages',
    (data, frame) => {
      if (!data || !data.eventType) return;
      const hidden = frame.headers['sqb-hidden-chat'] === 'true';
      if (hidden) {
        const chat = useChatStore.getState();
        chat.rememberHidden(data.conversationId, true);
        if (!chat.isPinUnlocked) return;
      }
      if (data.eventType === 'NEW_MESSAGE') {
        handleIncomingWsMessage(data, hidden);
      } else if (data.eventType === 'MESSAGE_REVOKED') {
        markMessageRevoked(data.conversationId, data.messageId);
      } else if (data.eventType === 'MESSAGE_READ') {
        handleIncomingReadReceipt(data);
      } else if (data.eventType === 'TYPING_STATUS') {
        handleIncomingTyping(data);
      } else if (data.eventType === 'GROUP_ROLES_CHANGED') {
        void queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.groupMembers(data.conversationId), exact: true });
        void refreshGroupHistory(queryClient, data.conversationId);
      } else if (data.eventType === 'GROUP_MEMBERS_ADDED') {
        void refreshJoinedGroup(queryClient, data.conversationId);
      } else if (data.eventType === 'GROUP_MEMBER_REMOVED' || data.eventType === 'GROUP_DETAILS_CHANGED') {
        const departedUserId = data.eventType === 'GROUP_MEMBER_REMOVED' ? data.removedUserId : data.leftUserId;
        if (departedUserId != null && departedUserId === useAuthStore.getState().user?.id) {
          void resetGroupHistory(queryClient, data.conversationId);
        } else {
          void refreshGroupHistory(queryClient, data.conversationId);
        }
        void refreshGroupMembership(queryClient, data.conversationId);
      }
    },
    isAuthenticated
  );

  useEffect(() => {
    if (!isAuthenticated || stompStatus !== 'CONNECTED') return;
    // Recover updates missed while the socket was disconnected.
    void queryClient.invalidateQueries({
      predicate: ({ queryKey }) => queryKey[0] === CHAT_QUERY_KEYS.conversations[0]
        || queryKey[0] === CHAT_QUERY_KEYS.allMessages[0]
        || (queryKey[0] === CHAT_QUERY_KEYS.groupCandidates[0] && typeof queryKey[1] === 'number'),
    });
  }, [isAuthenticated, queryClient, stompStatus]);
}
