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
    <div className="fixed bottom-3 right-4 z-40 flex items-end gap-3 pointer-events-none">
      {openFloatingChats.map((convId) => {
        const isMinimized = minimizedFloatingChats.includes(convId);
        const conv = findConversation(convId);
        const isGroup = conv?.type === 'GROUP';

        if (isMinimized) {
          return (
            <div
              key={convId}
              onClick={() => toggleMinimizeFloatingChat(convId)}
              className="pointer-events-auto flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl cursor-pointer hover:scale-105 transition-all group animate-in slide-in-from-bottom-3"
            >
              <div className="w-7 h-7 rounded-full overflow-hidden bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-bold">
                {conv?.displayAvatarUrl ? (
                  <img src={conv.displayAvatarUrl} alt="" className="w-full h-full object-cover" />
                ) : isGroup ? (
                  <Users className="w-4 h-4" />
                ) : (
                  conv?.displayName?.charAt(0) || 'U'
                )}
              </div>

              <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 max-w-[100px] truncate">
                {conv?.displayName || 'Trò chuyện'}
              </span>

              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  closeFloatingChat(convId);
                }}
                className="opacity-0 group-hover:opacity-100 p-0.5 rounded-full hover:bg-slate-200 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-600 transition-all"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          );
        }

        return (
          <div
            key={convId}
            className="pointer-events-auto w-80 sm:w-88 h-[460px] bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-bottom-5 duration-200"
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
                  title="Thu nhỏ"
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
                  title="Mở toàn màn hình"
                  className="p-1 rounded-lg hover:bg-white/20 transition-colors cursor-pointer"
                >
                  <Maximize2 className="w-3.5 h-3.5" />
                </button>

                <button
                  type="button"
                  onClick={() => closeFloatingChat(convId)}
                  title="Đóng cửa sổ"
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
