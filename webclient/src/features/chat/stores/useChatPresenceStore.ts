import { create } from 'zustand';
import { readAccessSessionId } from '@/api/accessSession';
import { useAuthStore } from '@/stores/useAuthStore';
import type { ChatPresenceDto } from '../types/chat.types';

interface ChatPresenceState {
  byConversation: Record<number, ChatPresenceDto>;
  updatePresence: (updates: ChatPresenceDto[]) => void;
  clearPresence: () => void;
}

export const useChatPresenceStore = create<ChatPresenceState>()((set) => ({
  byConversation: {},
  updatePresence: (updates) => set((state) => {
    const updated = { ...state.byConversation };
    for (const presence of updates) {
      if ((updated[presence.conversationId]?.observedAt ?? 0) <= presence.observedAt) {
        updated[presence.conversationId] = presence;
      }
    }
    return { byConversation: updated };
  }),
  clearPresence: () => set({ byConversation: {} }),
}));

useAuthStore.subscribe((state, previous) => {
  if (previous.tokenType === 'ACCESS' && (state.tokenType !== 'ACCESS' || state.user?.id !== previous.user?.id
      || readAccessSessionId(state.accessToken) !== readAccessSessionId(previous.accessToken))) {
    useChatPresenceStore.getState().clearPresence();
  }
});
