import { create } from 'zustand';
import { stompClient } from '@/lib/stompClient';
import { queryClient } from '@/lib/queryClient';
import { CHAT_QUERY_KEYS } from '../hooks/useChat';
import type {
  MessageType,
  UserSummaryDto,
  WsMessageBroadcastDto,
  WsReadReceiptBroadcastDto,
  WsTypingBroadcastDto,
  MessageDto,
} from '../types/chat.types';
import { useAuthStore } from '@/stores/useAuthStore';

interface ChatStoreState {
  // Cuộc trò chuyện đang chọn mở ở trang /messages
  activeConversationId: number | null;
  setActiveConversationId: (id: number | null) => void;

  // Các cửa sổ chat nổi (Floating Chat Widgets) ở góc dưới màn hình
  openFloatingChats: number[];
  minimizedFloatingChats: number[];
  openFloatingChat: (conversationId: number) => void;
  closeFloatingChat: (conversationId: number) => void;
  toggleMinimizeFloatingChat: (conversationId: number) => void;

  // Trạng thái mở khóa kho ẩn (Lưu trong RAM, tự khóa sau 15 phút hoặc khi reload)
  isPinUnlocked: boolean;
  unlockPin: () => void;
  lockPin: () => void;

  // Danh sách người dùng đang gõ theo từng cuộc hội thoại: conversationId -> userId -> fullName
  typingMap: Record<number, Record<number, { fullName: string; timer?: any }>>;
  setTypingUser: (conversationId: number, user: UserSummaryDto, isTyping: boolean) => void;

  // Các hành động gửi tin nhắn / STOMP
  sendMessage: (
    conversationId: number,
    content?: string,
    messageType?: MessageType,
    mediaUrls?: string[],
    replyToMessageId?: number
  ) => boolean;
  sendReadReceipt: (conversationId: number, messageId: number) => boolean;
  sendTyping: (conversationId: number, isTyping: boolean) => boolean;

  // Xử lý các gói tin WS nhận từ server
  handleIncomingWsMessage: (dto: WsMessageBroadcastDto) => void;
  handleIncomingReadReceipt: (dto: WsReadReceiptBroadcastDto) => void;
  handleIncomingTyping: (dto: WsTypingBroadcastDto) => void;
}

let pinLockTimeout: any = null;

export const useChatStore = create<ChatStoreState>()((set, get) => ({
  activeConversationId: null,
  setActiveConversationId: (id) => set({ activeConversationId: id }),

  openFloatingChats: [],
  minimizedFloatingChats: [],

  openFloatingChat: (conversationId) => {
    set((state) => {
      if (state.openFloatingChats.includes(conversationId)) {
        // Nếu đã mở và đang minimize thì un-minimize
        return {
          minimizedFloatingChats: state.minimizedFloatingChats.filter((id) => id !== conversationId),
        };
      }
      // Giới hạn tối đa 3 cửa sổ nổi cùng lúc
      const updated = [...state.openFloatingChats, conversationId];
      if (updated.length > 3) {
        updated.shift();
      }
      return {
        openFloatingChats: updated,
        minimizedFloatingChats: state.minimizedFloatingChats.filter((id) => id !== conversationId),
      };
    });
  },

  closeFloatingChat: (conversationId) => {
    set((state) => ({
      openFloatingChats: state.openFloatingChats.filter((id) => id !== conversationId),
      minimizedFloatingChats: state.minimizedFloatingChats.filter((id) => id !== conversationId),
    }));
  },

  toggleMinimizeFloatingChat: (conversationId) => {
    set((state) => {
      const isMinimized = state.minimizedFloatingChats.includes(conversationId);
      return {
        minimizedFloatingChats: isMinimized
          ? state.minimizedFloatingChats.filter((id) => id !== conversationId)
          : [...state.minimizedFloatingChats, conversationId],
      };
    });
  },

  isPinUnlocked: false,

  unlockPin: () => {
    if (pinLockTimeout) {
      clearTimeout(pinLockTimeout);
    }
    // Tự động khóa lại sau 15 phút
    pinLockTimeout = setTimeout(() => {
      set({ isPinUnlocked: false });
    }, 15 * 60 * 1000);

    set({ isPinUnlocked: true });
  },

  lockPin: () => {
    if (pinLockTimeout) {
      clearTimeout(pinLockTimeout);
      pinLockTimeout = null;
    }
    set({ isPinUnlocked: false });
  },

  typingMap: {},

  setTypingUser: (conversationId, user, isTyping) => {
    set((state) => {
      const convTyping = { ...(state.typingMap[conversationId] || {}) };

      if (!isTyping) {
        if (convTyping[user.userId]) {
          if (convTyping[user.userId].timer) {
            clearTimeout(convTyping[user.userId].timer);
          }
          delete convTyping[user.userId];
        }
      } else {
        if (convTyping[user.userId]?.timer) {
          clearTimeout(convTyping[user.userId].timer);
        }
        const timer = setTimeout(() => {
          get().setTypingUser(conversationId, user, false);
        }, 3000);

        convTyping[user.userId] = {
          fullName: user.fullName,
          timer,
        };
      }

      return {
        typingMap: {
          ...state.typingMap,
          [conversationId]: convTyping,
        },
      };
    });
  },

  sendMessage: (conversationId, content, messageType = 'TEXT', mediaUrls, replyToMessageId) => {
    const payload = {
      conversationId,
      content,
      messageType,
      mediaUrls: mediaUrls && mediaUrls.length > 0 ? mediaUrls : undefined,
      replyToMessageId: replyToMessageId || undefined,
    };
    return stompClient.send('/app/chat.send', payload);
  },

  sendReadReceipt: (conversationId, messageId) => {
    return stompClient.send('/app/chat.read', {
      conversationId,
      messageId,
    });
  },

  sendTyping: (conversationId, isTyping) => {
    return stompClient.send('/app/chat.typing', {
      conversationId,
      isTyping,
    });
  },

  handleIncomingWsMessage: (dto) => {
    const currentUserId = useAuthStore.getState().user?.id;
    const { activeConversationId, openFloatingChats } = get();

    // 1. Chuyển đổi payload thành MessageDto để bổ sung vào cache
    const newMessage: MessageDto = {
      messageId: dto.messageId,
      senderId: dto.sender.userId,
      senderName: dto.sender.fullName,
      avatarUrl: dto.sender.avatarUrl,
      frameUrl: dto.sender.frameUrl,
      content: dto.content || '',
      messageType: dto.messageType,
      mediaUrls: dto.mediaUrls || [],
      replyToMessage: null,
      createdAt: dto.createdAt,
      isEdited: dto.isEdited,
      isRevoked: false,
    };

    // 2. Cập nhật cache của danh sách tin nhắn nếu cuộc hội thoại đang mở
    queryClient.setQueryData(
      CHAT_QUERY_KEYS.messages(dto.conversationId),
      (oldData: any) => {
        if (!oldData || !oldData.pages || oldData.pages.length === 0) return oldData;
        const firstPage = oldData.pages[0];
        // Tránh trùng tin nhắn
        const exists = firstPage.messages.some((m: MessageDto) => m.messageId === dto.messageId);
        if (exists) return oldData;

        const updatedPages = [...oldData.pages];
        updatedPages[0] = {
          ...firstPage,
          messages: [newMessage, ...firstPage.messages],
        };
        return {
          ...oldData,
          pages: updatedPages,
        };
      }
    );

    // 3. Cập nhật danh sách hội thoại ['conversations']
    queryClient.setQueryData(CHAT_QUERY_KEYS.conversations, (oldData: any) => {
      if (!oldData || !oldData.pages) return oldData;

      const isCurrentActive =
        activeConversationId === dto.conversationId ||
        openFloatingChats.includes(dto.conversationId);

      let found = false;
      const updatedPages = oldData.pages.map((page: any) => {
        const items = page.items.map((conv: any) => {
          if (conv.conversationId === dto.conversationId) {
            found = true;
            return {
              ...conv,
              lastMessage: {
                messageId: dto.messageId,
                senderId: dto.sender.userId,
                senderName: dto.sender.fullName,
                content: dto.content || (dto.messageType === 'IMAGE' ? '[Hình ảnh]' : '[Tập tin]'),
                messageType: dto.messageType,
                createdAt: dto.createdAt,
              },
              unreadCount:
                isCurrentActive || dto.sender.userId === currentUserId
                  ? 0
                  : (conv.unreadCount || 0) + 1,
              updatedAt: dto.createdAt,
            };
          }
          return conv;
        });
        return { ...page, items };
      });

      // Nếu hội thoại mới chưa có trong cache thì invalidate để tải lại
      if (!found) {
        queryClient.invalidateQueries({ queryKey: CHAT_QUERY_KEYS.conversations });
      }

      return {
        ...oldData,
        pages: updatedPages,
      };
    });

    // 4. Nếu người dùng đang mở hội thoại này và tin nhắn không phải của chính mình -> tự gửi biên nhận đã đọc
    if (
      (activeConversationId === dto.conversationId ||
        openFloatingChats.includes(dto.conversationId)) &&
      dto.sender.userId !== currentUserId
    ) {
      get().sendReadReceipt(dto.conversationId, dto.messageId);
    }
  },

  handleIncomingReadReceipt: (dto) => {
    // Cập nhật trạng thái đã đọc nếu cần
    console.debug('[ChatStore] Message read receipt received:', dto);
  },

  handleIncomingTyping: (dto) => {
    const currentUserId = useAuthStore.getState().user?.id;
    if (dto.user.userId === currentUserId) return;
    get().setTypingUser(dto.conversationId, dto.user, dto.isTyping);
  },
}));
