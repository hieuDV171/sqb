import { create } from 'zustand';
import type { InfiniteData } from '@tanstack/react-query';
import { readAccessSessionId } from '@/api/accessSession';
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
  MessageListResponseDto,
  ConversationListResponseDto,
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
  hiddenConversationIds: number[];
  rememberHidden: (id: number, hidden: boolean) => void;
  unlockPin: (expiresAt: string) => void;
  lockPin: () => void;

  // Danh sách người dùng đang gõ theo từng cuộc hội thoại: conversationId -> userId -> fullName
  typingMap: Record<number, Record<number, { fullName: string; timer?: ReturnType<typeof setTimeout> }>>;
  setTypingUser: (conversationId: number, user: UserSummaryDto, typing: boolean) => void;

  // Các hành động gửi tin nhắn / STOMP
  sendMessage: (
    conversationId: number,
    content?: string,
    messageType?: MessageType,
    mediaUrls?: string[],
    replyToMessageId?: number
  ) => boolean;
  sendReadReceipt: (conversationId: number, messageId: number) => boolean;
  sendTyping: (conversationId: number, typing: boolean) => boolean;

  // Xử lý các gói tin WS nhận từ server
  handleIncomingWsMessage: (dto: WsMessageBroadcastDto, hidden?: boolean) => void;
  markMessageRevoked: (conversationId: number, messageId: number) => void;
  handleIncomingReadReceipt: (dto: WsReadReceiptBroadcastDto) => void;
  handleIncomingTyping: (dto: WsTypingBroadcastDto) => void;
}

let pinLockTimeout: ReturnType<typeof setTimeout> | null = null;

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
  hiddenConversationIds: [],
  rememberHidden: (id, hidden) => {
    const ids = get().hiddenConversationIds;
    if (ids.includes(id) !== hidden) {
      set({ hiddenConversationIds: hidden ? [...ids, id] : ids.filter((value) => value !== id) });
      if (hidden) {
        queryClient.setQueryData<InfiniteData<ConversationListResponseDto>>(CHAT_QUERY_KEYS.conversations, (current) =>
          current && {
            ...current,
            pages: current.pages.map((page) => ({ ...page, items: page.items.filter((item) => item.conversationId !== id) })),
          });
      }
    }
  },

  unlockPin: (expiresAt) => {
    if (pinLockTimeout) {
      clearTimeout(pinLockTimeout);
    }
    const remaining = Date.parse(expiresAt) - Date.now();
    if (!Number.isFinite(remaining) || remaining <= 0) {
      get().lockPin();
      return;
    }
    pinLockTimeout = setTimeout(() => get().lockPin(), Math.min(remaining, 15 * 60 * 1000));

    set({ isPinUnlocked: true });
  },

  lockPin: () => {
    if (pinLockTimeout) {
      clearTimeout(pinLockTimeout);
      pinLockTimeout = null;
    }
    const { hiddenConversationIds, openFloatingChats, minimizedFloatingChats, typingMap } = get();
    const hidden = new Set(hiddenConversationIds);
    set({
      isPinUnlocked: false,
      openFloatingChats: openFloatingChats.filter((id) => !hidden.has(id)),
      minimizedFloatingChats: minimizedFloatingChats.filter((id) => !hidden.has(id)),
      typingMap: Object.fromEntries(Object.entries(typingMap).filter(([id]) => !hidden.has(Number(id)))),
    });
    queryClient.removeQueries({ queryKey: CHAT_QUERY_KEYS.hiddenConversations, exact: true });
    for (const id of hidden) {
      queryClient.removeQueries({ queryKey: CHAT_QUERY_KEYS.messages(id), exact: true });
      queryClient.removeQueries({ queryKey: CHAT_QUERY_KEYS.groupMembers(id), exact: true });
    }
  },

  typingMap: {},

  setTypingUser: (conversationId, user, typing) => {
    set((state) => {
      const convTyping = { ...(state.typingMap[conversationId] || {}) };

      if (!typing) {
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
        }, 6500);

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

  sendTyping: (conversationId, typing) => {
    return stompClient.send('/app/chat.typing', {
      conversationId,
      typing,
    });
  },

  handleIncomingWsMessage: (dto, hidden = false) => {
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
      replyToMessage: dto.replyToMessage ?? (dto.replyToMessageId ? {
        messageId: dto.replyToMessageId,
        contentPreview: 'Xem tin nhắn gốc',
      } : null),
      createdAt: dto.createdAt,
      isEdited: dto.isEdited,
      revoked: false,
    };

    // 2. Cập nhật cache của danh sách tin nhắn nếu cuộc hội thoại đang mở
    queryClient.setQueryData<InfiniteData<MessageListResponseDto>>(
      CHAT_QUERY_KEYS.messages(dto.conversationId),
      (oldData) => {
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
    const listKey = hidden ? CHAT_QUERY_KEYS.hiddenConversations : CHAT_QUERY_KEYS.conversations;
    queryClient.setQueryData<InfiniteData<ConversationListResponseDto>>(listKey, (oldData) => {
      if (!oldData || !oldData.pages) return oldData;

      const isCurrentActive =
        activeConversationId === dto.conversationId ||
        openFloatingChats.includes(dto.conversationId);

      let found = false;
      const updatedPages = oldData.pages.map((page) => {
        const items = page.items.map((conv) => {
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
        queryClient.invalidateQueries({ queryKey: listKey });
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

  markMessageRevoked: (conversationId, messageId) => {
    const content = 'Tin nhắn đã bị thu hồi';
    const messagesKey = CHAT_QUERY_KEYS.messages(conversationId);
    const conversationKeys = [CHAT_QUERY_KEYS.conversations, CHAT_QUERY_KEYS.hiddenConversations];
    const fetchingKeys = [messagesKey, ...conversationKeys]
      .filter((key) => queryClient.getQueryState(key)?.fetchStatus === 'fetching');

    // A request started before the revocation may still contain the original message.
    for (const queryKey of fetchingKeys) {
      void queryClient.cancelQueries({ queryKey, exact: true }, { revert: false });
    }

    queryClient.setQueryData<InfiniteData<MessageListResponseDto>>(messagesKey, (current) => current && ({
      ...current,
      pages: current.pages.map((page) => ({
        ...page,
        messages: page.messages.map((message) => {
          if (message.messageId === messageId) {
            return { ...message, content, revoked: true, mediaUrls: [], replyToMessage: null };
          }
          const quote = message.replyToMessage;
          return quote?.messageId === messageId && !quote.unavailable
            ? { ...message, replyToMessage: { ...quote, contentPreview: content } }
            : message;
        }),
      })),
    }));

    for (const key of conversationKeys) {
      queryClient.setQueryData<InfiniteData<ConversationListResponseDto>>(key, (current) => current && ({
        ...current,
        pages: current.pages.map((page) => ({
          ...page,
          items: page.items.map((conversation) => {
            const lastMessage = conversation.lastMessage;
            return conversation.conversationId === conversationId && lastMessage?.messageId === messageId
              ? { ...conversation, lastMessage: { ...lastMessage, content, revoked: true } }
              : conversation;
          }),
        })),
      }));
    }
    for (const queryKey of fetchingKeys) {
      void queryClient.invalidateQueries({ queryKey, exact: true });
    }
  },

  handleIncomingReadReceipt: (dto) => {
    // Cập nhật trạng thái đã đọc nếu cần
    console.debug('[ChatStore] Message read receipt received:', dto);
  },

  handleIncomingTyping: (dto) => {
    const currentUserId = useAuthStore.getState().user?.id;
    if (dto.user.userId === currentUserId) return;
    get().setTypingUser(dto.conversationId, dto.user, dto.typing);
  },
}));

useAuthStore.subscribe((state, previous) => {
  if (previous.tokenType === 'ACCESS' && (state.tokenType !== 'ACCESS' || state.user?.id !== previous.user?.id
      || readAccessSessionId(state.accessToken) !== readAccessSessionId(previous.accessToken))) {
    useChatStore.getState().lockPin();
    useChatStore.setState({ hiddenConversationIds: [], openFloatingChats: [], minimizedFloatingChats: [], activeConversationId: null });
  }
});
