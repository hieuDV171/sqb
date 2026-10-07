import { useState, useMemo } from 'react';
import { useConversations, useGroupMembers, useHideConversation } from '../hooks/useChat';
import { useConversationTypingListener } from '../hooks/useChatRealtime';
import { useChatStore } from '../stores/useChatStore';
import { MessageList } from './MessageList';
import { ChatInput } from './ChatInput';
import type { MessageDto } from '../types/chat.types';
import {
  Users,
  ChevronLeft,
  EyeOff,
  X,
} from 'lucide-react';

interface ChatWindowProps {
  conversationId: number;
  onBack?: () => void;
  onOpenGroupMembers?: () => void;
  onOpenDeleteModal?: (message: MessageDto) => void;
}

export function ChatWindow({
  conversationId,
  onBack,
  onOpenGroupMembers,
  onOpenDeleteModal,
}: ChatWindowProps) {
  // Đăng ký nhận sự kiện typing thời gian thực trên topic /topic/conv.{conversationId}.typing
  useConversationTypingListener(conversationId);

  const { typingMap } = useChatStore();
  const [replyTo, setReplyTo] = useState<MessageDto | null>(null);
  const [previewImageUrl, setPreviewImageUrl] = useState<string | null>(null);

  // Lấy thông tin hội thoại từ cache của danh sách
  const { data: conversationsData } = useConversations();
  const hideMutation = useHideConversation();

  const conversation = useMemo(() => {
    if (!conversationsData?.pages) return null;
    for (const page of conversationsData.pages) {
      const found = page.items.find((c) => c.conversationId === conversationId);
      if (found) return found;
    }
    return null;
  }, [conversationsData, conversationId]);

  const isGroup = conversation?.type === 'GROUP';
  const { data: members = [] } = useGroupMembers(conversationId, isGroup);

  // Danh sách người đang gõ trong cuộc trò chuyện này
  const typingUsers = useMemo(() => {
    const map = typingMap[conversationId];
    if (!map) return [];
    return Object.values(map).map((u) => u.fullName);
  }, [typingMap, conversationId]);

  const handleHide = () => {
    if (window.confirm('Bạn có muốn ẩn cuộc hội thoại này vào kho bí mật?')) {
      hideMutation.mutate(conversationId);
      onBack?.();
    }
  };

  return (
    <div className="flex flex-col h-full bg-slate-50/50 dark:bg-slate-950/50 relative overflow-hidden">
      {/* Chat Header */}
      <div className="h-16 px-4 border-b border-slate-200/80 dark:border-slate-800 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md flex items-center justify-between gap-3 shrink-0 z-10 shadow-2xs">
        <div className="flex items-center gap-3 min-w-0">
          {/* Mobile Back Button */}
          {onBack && (
            <button
              type="button"
              onClick={onBack}
              className="md:hidden p-2 -ml-1 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              aria-label="Quay lại danh sách"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
          )}

          {/* Avatar */}
          <div className="relative shrink-0">
            <div className="w-10 h-10 rounded-full overflow-hidden bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white font-semibold text-sm shadow-xs">
              {conversation?.displayAvatarUrl ? (
                <img
                  src={conversation.displayAvatarUrl}
                  alt={conversation.displayName}
                  className="w-full h-full object-cover"
                />
              ) : isGroup ? (
                <Users className="w-5 h-5 text-white/90" />
              ) : (
                conversation?.displayName?.charAt(0).toUpperCase() || 'U'
              )}
            </div>
            {/* Online Indicator */}
            <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-emerald-500 ring-2 ring-white dark:ring-slate-900" />
          </div>

          {/* Title & Subtitle */}
          <div className="flex flex-col min-w-0">
            <h3 className="text-sm sm:text-base font-bold text-slate-900 dark:text-slate-100 truncate">
              {conversation?.displayName || 'Đang tải thông tin...'}
            </h3>

            {/* Subtitle / Typing notification */}
            <div className="text-xs text-slate-500 dark:text-slate-400 truncate">
              {typingUsers.length > 0 ? (
                <span className="text-indigo-600 dark:text-indigo-400 font-medium animate-pulse">
                  {typingUsers.join(', ')} đang soạn tin...
                </span>
              ) : isGroup ? (
                <span>
                  {members.length > 0 ? `${members.length} thành viên` : `${conversation?.memberCount || 0} thành viên`}
                </span>
              ) : (
                <span className="text-emerald-600 dark:text-emerald-400 font-medium">Đang hoạt động</span>
              )}
            </div>
          </div>
        </div>

        {/* Header Right Actions */}
        <div className="flex items-center gap-1 shrink-0">
          {isGroup && onOpenGroupMembers && (
            <button
              type="button"
              onClick={onOpenGroupMembers}
              title="Xem danh sách thành viên"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer"
            >
              <Users className="w-5 h-5" />
            </button>
          )}

          <button
            type="button"
            onClick={handleHide}
            title="Ẩn hội thoại vào kho bí mật"
            className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-amber-600 dark:hover:text-amber-400 transition-colors cursor-pointer"
          >
            <EyeOff className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Message List Area */}
      <MessageList
        conversationId={conversationId}
        isGroup={isGroup}
        onReplyMessage={(msg) => setReplyTo(msg)}
        onDeleteMessage={(msg) => onOpenDeleteModal?.(msg)}
        onImageClick={(url) => setPreviewImageUrl(url)}
      />

      {/* Chat Input */}
      <ChatInput
        conversationId={conversationId}
        replyTo={replyTo}
        onCancelReply={() => setReplyTo(null)}
      />

      {/* Image Lightbox Modal */}
      {previewImageUrl && (
        <div
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in"
          onClick={() => setPreviewImageUrl(null)}
        >
          <button
            type="button"
            onClick={() => setPreviewImageUrl(null)}
            className="absolute top-4 right-4 p-2 rounded-full bg-black/50 text-white hover:bg-black/80 transition-colors cursor-pointer"
          >
            <X className="w-6 h-6" />
          </button>
          <img
            src={previewImageUrl}
            alt="Phóng to ảnh"
            className="max-w-full max-h-[90vh] object-contain rounded-xl shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
}
