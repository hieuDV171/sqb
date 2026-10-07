import { useState } from 'react';
import type { ConversationDto } from '../types/chat.types';
import { formatChatTime, truncate } from '../utils/chatUtils';
import { useAuthStore } from '@/stores/useAuthStore';
import {
  Users,
  Lock,
  MoreVertical,
  BellOff,
  EyeOff,
  Eye,
  LogOut,
  Image as ImageIcon,
  FileText,
} from 'lucide-react';

interface ConversationItemProps {
  conversation: ConversationDto;
  isActive: boolean;
  isHiddenMode?: boolean;
  onClick: () => void;
  onHide?: (conversationId: number) => void;
  onUnhide?: (conversationId: number) => void;
  onLeave?: (conversationId: number) => void;
}

export function ConversationItem({
  conversation,
  isActive,
  isHiddenMode = false,
  onClick,
  onHide,
  onUnhide,
  onLeave,
}: ConversationItemProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [showMenu, setShowMenu] = useState(false);

  const isGroup = conversation.type === 'GROUP';
  const hasUnread = conversation.unreadCount > 0 && !isActive;

  const renderLastMessage = () => {
    if (!conversation.lastMessage) {
      return (
        <span className="text-slate-400 dark:text-slate-500 italic text-xs">
          Chưa có tin nhắn nào
        </span>
      );
    }

    const { senderId, senderName, content, messageType } = conversation.lastMessage;
    const isMe = senderId === currentUserId;
    const prefix = isMe ? 'Bạn: ' : isGroup ? `${senderName.split(' ').pop()}: ` : '';

    if (messageType === 'IMAGE') {
      return (
        <span className="inline-flex items-center gap-1 text-slate-500 dark:text-slate-400 text-xs">
          <ImageIcon className="w-3.5 h-3.5 text-indigo-500" />
          {prefix}[Hình ảnh]
        </span>
      );
    }

    if (messageType === 'FILE') {
      return (
        <span className="inline-flex items-center gap-1 text-slate-500 dark:text-slate-400 text-xs">
          <FileText className="w-3.5 h-3.5 text-amber-500" />
          {prefix}[Tập tin]
        </span>
      );
    }

    return (
      <span
        className={`text-xs truncate ${
          hasUnread
            ? 'font-semibold text-slate-900 dark:text-slate-100'
            : 'text-slate-500 dark:text-slate-400'
        }`}
      >
        {prefix}
        {truncate(content, 32)}
      </span>
    );
  };

  return (
    <div
      onClick={onClick}
      className={`group relative flex items-center gap-3 px-3.5 py-3 rounded-2xl cursor-pointer transition-all duration-200 select-none ${
        isActive
          ? 'bg-indigo-50/90 dark:bg-indigo-950/40 text-slate-900 dark:text-slate-50 shadow-xs ring-1 ring-indigo-500/20'
          : 'hover:bg-slate-100/80 dark:hover:bg-slate-900/60 text-slate-700 dark:text-slate-300'
      }`}
    >
      {/* Active Bar indicator */}
      {isActive && (
        <div className="absolute left-0 top-3 bottom-3 w-1 bg-indigo-600 dark:bg-indigo-500 rounded-r-full" />
      )}

      {/* Avatar Container */}
      <div className="relative shrink-0">
        <div className="w-12 h-12 rounded-full overflow-hidden bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white font-semibold text-base shadow-xs">
          {conversation.displayAvatarUrl ? (
            <img
              src={conversation.displayAvatarUrl}
              alt={conversation.displayName}
              className="w-full h-full object-cover"
            />
          ) : isGroup ? (
            <Users className="w-6 h-6 text-white/90" />
          ) : (
            conversation.displayName?.charAt(0).toUpperCase() || 'U'
          )}
        </div>

        {/* Group / Lock Badge on Avatar */}
        {isGroup ? (
          <div
            title={`Nhóm ${conversation.memberCount} thành viên`}
            className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-slate-100 dark:bg-slate-800 border border-white dark:border-slate-900 flex items-center justify-center text-slate-600 dark:text-slate-300 text-[10px]"
          >
            <Users className="w-3 h-3" />
          </div>
        ) : conversation.hiddenAt ? (
          <div
            title="Kho trò chuyện ẩn"
            className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-amber-500 border border-white dark:border-slate-900 flex items-center justify-center text-white"
          >
            <Lock className="w-2.5 h-2.5" />
          </div>
        ) : null}
      </div>

      {/* Details: Name, Last Message, Time, Badges */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-1 mb-1">
          <div className="flex items-center gap-1.5 min-w-0">
            <h4
              className={`text-sm truncate font-medium ${
                hasUnread
                  ? 'font-bold text-slate-900 dark:text-slate-50'
                  : 'text-slate-800 dark:text-slate-200'
              }`}
            >
              {conversation.displayName}
            </h4>
            {conversation.isMuted && (
              <BellOff className="w-3 h-3 text-slate-400 shrink-0" />
            )}
          </div>

          <span
            className={`text-[11px] shrink-0 ${
              hasUnread
                ? 'font-bold text-indigo-600 dark:text-indigo-400'
                : 'text-slate-400 dark:text-slate-500'
            }`}
          >
            {formatChatTime(conversation.updatedAt)}
          </span>
        </div>

        <div className="flex items-center justify-between gap-2">
          <div className="flex-1 min-w-0">{renderLastMessage()}</div>

          {/* Unread badge */}
          {hasUnread && (
            <span className="shrink-0 min-w-5 h-5 px-1.5 rounded-full bg-indigo-600 dark:bg-indigo-500 text-white text-[11px] font-bold flex items-center justify-center shadow-xs">
              {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
            </span>
          )}
        </div>
      </div>

      {/* More Options Button */}
      <div
        className="relative shrink-0"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={() => setShowMenu((prev) => !prev)}
          className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-200/60 dark:hover:bg-slate-800 transition-all cursor-pointer"
          aria-label="Tùy chọn hội thoại"
        >
          <MoreVertical className="w-4 h-4" />
        </button>

        {showMenu && (
          <>
            <div
              className="fixed inset-0 z-20"
              onClick={() => setShowMenu(false)}
            />
            <div className="absolute right-0 top-8 z-30 w-44 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-lg p-1 text-xs select-none animate-in fade-in zoom-in-95">
              {isHiddenMode ? (
                <button
                  type="button"
                  onClick={() => {
                    setShowMenu(false);
                    onUnhide?.(conversation.conversationId);
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2 text-left rounded-lg text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <Eye className="w-4 h-4 text-emerald-500" />
                  Bỏ ẩn hội thoại
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    setShowMenu(false);
                    onHide?.(conversation.conversationId);
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2 text-left rounded-lg text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <EyeOff className="w-4 h-4 text-amber-500" />
                  Ẩn vào kho bí mật
                </button>
              )}

              {isGroup && onLeave && (
                <button
                  type="button"
                  onClick={() => {
                    setShowMenu(false);
                    onLeave(conversation.conversationId);
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2 text-left rounded-lg text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  Rời khỏi nhóm
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
