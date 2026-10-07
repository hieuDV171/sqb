import { useStompSubscription } from '@/lib/stompClient';
import { useAuthStore } from '@/stores/useAuthStore';
import { useChatStore } from '../stores/useChatStore';
import type { WsTypingBroadcastDto } from '../types/chat.types';

/**
 * Hook lắng nghe tin nhắn và biên nhận đã đọc toàn cục của người dùng
 * Đích đến: /user/queue/messages
 */
export function useChatRealtimeListener() {
  const { isAuthenticated } = useAuthStore();
  const handleIncomingWsMessage = useChatStore((s) => s.handleIncomingWsMessage);
  const handleIncomingReadReceipt = useChatStore((s) => s.handleIncomingReadReceipt);

  useStompSubscription<any>(
    '/user/queue/messages',
    (data) => {
      if (!data || !data.eventType) return;
      if (data.eventType === 'NEW_MESSAGE') {
        handleIncomingWsMessage(data);
      } else if (data.eventType === 'MESSAGE_READ') {
        handleIncomingReadReceipt(data);
      }
    },
    isAuthenticated
  );
}

/**
 * Hook lắng nghe trạng thái đang gõ phím theo cuộc trò chuyện cụ thể
 * Đích đến: /topic/conv.{conversationId}.typing
 */
export function useConversationTypingListener(conversationId: number | null) {
  const handleIncomingTyping = useChatStore((s) => s.handleIncomingTyping);

  useStompSubscription<WsTypingBroadcastDto>(
    conversationId ? `/topic/conv.${conversationId}.typing` : null,
    (data) => {
      if (data && data.eventType === 'TYPING_STATUS') {
        handleIncomingTyping(data);
      }
    },
    !!conversationId
  );
}
