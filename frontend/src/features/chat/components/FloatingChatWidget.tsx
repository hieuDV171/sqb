import { getAvatarInitial } from '@/lib/avatar';
import { useNavigate } from 'react-router-dom';
import { useChatStore } from '../stores/useChatStore';
import { useConversations } from '../hooks/useChat';
import { ChatWindow } from './ChatWindow';
import {
  Minus,
  Maximize2,
  X,
  Users,
  MessageSquare,
} from 'lucide-react';

export function FloatingChatWidget() {
  const navigate = useNavigate();
  const {
    openFloatingChats,
    minimizedFloatingChats,
    closeFloatingChat,
    toggleMinimizeFloatingChat,
  } = useChatStore();

  const { data: conversationsData } = useConversations();

  if (openFloatingChats.length === 0) return null;

  const findConversation = (id: number) => {
    if (!conversationsData?.pages) return null;
    for (const page of conversationsData.pages) {
      const found = page.items.find((c) => c.conversationId === id);
      if (found) return found;
    }
    return null;
  };

  return (
    <div className="pointer-events-none fixed right-4 bottom-3 z-40 hidden max-w-[calc(100vw-2rem)] items-end gap-3 md:flex">
      {openFloatingChats.map((convId) => {
        const isMinimized = minimizedFloatingChats.includes(convId);
        const conv = findConversation(convId);
        const isGroup = conv?.type === 'GROUP';

        if (isMinimized) {
          return (
            <div
              key={convId}
              className="pointer-events-auto flex items-center gap-1 rounded-full border border-border bg-card p-1 shadow-lg"
            >
              <button type="button" onClick={() => toggleMinimizeFloatingChat(convId)}
                className="flex min-h-11 cursor-pointer items-center gap-2 rounded-full px-2 hover:bg-muted"
                aria-label={`Mở ${conv?.displayName || 'cuộc trò chuyện'}`}>
              <span className="grid size-7 place-items-center overflow-hidden rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                {conv?.displayAvatarUrl ? (
                  <img src={conv.displayAvatarUrl} alt="" className="w-full h-full object-cover" />
                ) : isGroup ? (
                  <Users className="w-4 h-4" />
                ) : (
                  getAvatarInitial(conv?.displayName)
                )}
              </span>

              <span className="max-w-24 truncate text-sm font-semibold text-foreground">
                {conv?.displayName || 'Trò chuyện'}
              </span>
              </button>

              <button
                type="button"
                onClick={() => closeFloatingChat(convId)}
                aria-label="Đóng hội thoại"
                className="grid size-11 place-items-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          );
        }

        return (
          <div
            key={convId}
            className="pointer-events-auto w-80 sm:w-88 h-[460px] bg-card rounded-3xl border border-border shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-bottom-5 duration-200"
          >
            {/* Widget Top Bar Controls */}
            <div className="h-10 px-3 bg-indigo-600 dark:bg-slate-850 flex items-center justify-between text-white shrink-0">
              <div className="flex items-center gap-2 truncate">
                <MessageSquare className="w-4 h-4 text-indigo-200 shrink-0" />
                <span className="text-xs font-bold truncate">
                  {conv?.displayName || 'Đang trò chuyện'}
                </span>
              </div>

              <div className="flex items-center gap-1 shrink-0">
                <button
                  type="button"
                  onClick={() => toggleMinimizeFloatingChat(convId)}
                  title="Thu nhỏ" aria-label="Thu nhỏ hội thoại"
                  className="p-1 rounded-lg hover:bg-white/20 transition-colors cursor-pointer"
                >
                  <Minus className="w-3.5 h-3.5" />
                </button>

                <button
                  type="button"
                  onClick={() => {
                    closeFloatingChat(convId);
                    navigate(`/messages/${convId}`);
                  }}
                  title="Mở toàn màn hình" aria-label="Mở hội thoại toàn màn hình"
                  className="p-1 rounded-lg hover:bg-white/20 transition-colors cursor-pointer"
                >
                  <Maximize2 className="w-3.5 h-3.5" />
                </button>

                <button
                  type="button"
                  onClick={() => closeFloatingChat(convId)}
                  title="Đóng cửa sổ" aria-label="Đóng cửa sổ hội thoại"
                  className="p-1 rounded-lg hover:bg-white/20 transition-colors cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Embedded Chat Window */}
            <div className="flex-1 overflow-hidden">
              <ChatWindow conversationId={convId} />
            </div>
          </div>
        );
      })}
    </div>
  );
}
