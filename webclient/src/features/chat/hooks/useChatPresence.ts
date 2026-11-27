import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import { stompClient, useStompStatus, useStompSubscription } from '@/lib/stompClient';
import { useAuthStore } from '@/stores/useAuthStore';
import { chatService } from '@/features/chat';
import { useChatStore } from '@/features/chat';
import { useChatPresenceStore } from '../stores/useChatPresenceStore';
import type { ChatPresenceDto } from '@/features/chat';
import { useConversations } from './useChat';

const HEARTBEAT_INTERVAL_MS = 15_000;
const PRESENCE_BATCH_SIZE = 100;

export function useChatPresence() {
  const stompStatus = useStompStatus();
  const userId = useAuthStore((state) => state.user?.id);
  const updatePresence = useChatPresenceStore((state) => state.updatePresence);
  const clearPresence = useChatPresenceStore((state) => state.clearPresence);
  const activeConversationId = useChatStore((state) => state.activeConversationId);
  const openFloatingChats = useChatStore((state) => state.openFloatingChats);
  const { pathname } = useLocation();
  const isInbox = pathname === '/messages' || pathname.startsWith('/messages/');
  const isConnected = userId != null && stompStatus === 'CONNECTED';
  const { data: conversations } = useConversations();
  const visibleConversationIds = new Set<number>(openFloatingChats);
  if (isInbox) {
    if (activeConversationId != null) visibleConversationIds.add(activeConversationId);
    for (const page of conversations?.pages ?? []) {
      for (const conversation of page.items) {
        if (conversation.type === 'DIRECT') visibleConversationIds.add(conversation.conversationId);
      }
    }
  }
  const conversationIds = [...visibleConversationIds].sort((left, right) => left - right);

  useStompSubscription<ChatPresenceDto[]>('/user/queue/chat-presence', updatePresence, userId != null);

  useEffect(() => {
    if (!isConnected) {
      clearPresence();
      return;
    }

    let timer: number | undefined;
    const sendHeartbeat = (active: boolean) => {
      if (stompClient.getStatus() === 'CONNECTED') stompClient.send('/app/chat.presence', { active });
    };
    const pause = () => {
      window.clearInterval(timer);
      sendHeartbeat(false);
    };
    const refreshVisibility = () => {
      window.clearInterval(timer);
      if (document.visibilityState === 'visible') {
        sendHeartbeat(true);
        timer = window.setInterval(() => sendHeartbeat(true), HEARTBEAT_INTERVAL_MS);
      } else {
        sendHeartbeat(false);
      }
    };
    refreshVisibility();
    document.addEventListener('visibilitychange', refreshVisibility);
    window.addEventListener('pagehide', pause);
    window.addEventListener('pageshow', refreshVisibility);
    return () => {
      pause();
      document.removeEventListener('visibilitychange', refreshVisibility);
      window.removeEventListener('pagehide', pause);
      window.removeEventListener('pageshow', refreshVisibility);
    };
  }, [clearPresence, isConnected, userId]);

  const { data: snapshot, isFetching: isFetchingSnapshot } = useQuery({
    queryKey: ['chat-presence', userId, conversationIds],
    enabled: isConnected && conversationIds.length > 0,
    queryFn: async ({ signal }) => {
      const requests = [];
      for (let index = 0; index < conversationIds.length; index += PRESENCE_BATCH_SIZE) {
        requests.push(chatService.getPresence(conversationIds.slice(index, index + PRESENCE_BATCH_SIZE), signal));
      }
      return (await Promise.all(requests)).flatMap((response) => response.data);
    },
    staleTime: 0,
    refetchOnWindowFocus: true,
    refetchInterval: isConnected ? 30_000 : false,
  });

  useEffect(() => {
    if (isConnected && !isFetchingSnapshot && snapshot) updatePresence(snapshot);
  }, [isConnected, isFetchingSnapshot, snapshot, updatePresence]);
}
