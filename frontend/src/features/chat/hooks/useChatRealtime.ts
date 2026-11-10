import { useStompSubscription } from '@/lib/stompClient';
import { useAuthStore } from '@/stores/useAuthStore';
import { useChatStore } from '../stores/useChatStore';
import type { WsMessageBroadcastDto, WsReadReceiptBroadcastDto, WsTypingBroadcastDto } from '../types/chat.types';

/**
 * Hook lắng nghe tin nhắn và biên nhận đã đọc toàn cục của người dùng
 * Đích đến: /user/queue/messages
 */
export function useChatRealtimeListener() {
  const { isAuthenticated } = useAuthStore();
  const handleIncomingWsMessage = useChatStore((s) => s.handleIncomingWsMessage);
  const handleIncomingReadReceipt = useChatStore((s) => s.handleIncomingReadReceipt);
  const handleIncomingTyping = useChatStore((s) => s.handleIncomingTyping);

  useStompSubscription<WsMessageBroadcastDto | WsReadReceiptBroadcastDto | WsTypingBroadcastDto>(
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
      } else if (data.eventType === 'MESSAGE_READ') {
        handleIncomingReadReceipt(data);
      } else if (data.eventType === 'TYPING_STATUS') {
        handleIncomingTyping(data);
      }
    },
    isAuthenticated
  );
}
