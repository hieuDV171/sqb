import React, { useEffect, useRef, useMemo } from 'react';
import { useMessages } from '../hooks/useChat';
import { useAuthStore } from '@/stores/useAuthStore';
import { MessageBubble } from './MessageBubble';
import { formatDateDivider } from '../utils/chatUtils';
import type { MessageDto } from '../types/chat.types';
import { Loader2, MessageSquareDashed } from 'lucide-react';

interface MessageListProps {
  conversationId: number;
  isGroup: boolean;
  onReplyMessage: (message: MessageDto) => void;
  onDeleteMessage: (message: MessageDto) => void;
  onImageClick?: (url: string) => void;
}

export function MessageList({
  conversationId,
  isGroup,
  onReplyMessage,
  onDeleteMessage,
  onImageClick,
}: MessageListProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const bottomRef = useRef<HTMLDivElement>(null);
  const topSentinelRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const {
    data,
    isLoading,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
  } = useMessages(conversationId);

  // Gom toàn bộ tin nhắn từ các trang phân trang (API trả về tin mới nhất trước)
  const allMessages = useMemo(() => {
    if (!data?.pages) return [];
    const flat = data.pages.flatMap((page) => page.messages);
    // Đảo ngược mảng để render từ cũ nhất đến mới nhất theo thứ tự đọc
    return [...flat].reverse();
  }, [data]);

  // Tự động cuộn xuống dưới cùng khi có tin nhắn mới hoặc khi vừa mở hội thoại
  const lastMessageId = allMessages.length > 0 ? allMessages[allMessages.length - 1].messageId : null;
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [conversationId, lastMessageId]);

  // Lắng nghe khi người dùng cuộn lên trên cùng để load tin nhắn cũ hơn
  useEffect(() => {
    const sentinel = topSentinelRef.current;
    if (!sentinel || !hasNextPage || isFetchingNextPage) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.1 }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  if (isLoading) {
    return (
      <div className="flex-1 flex flex-col justify-end p-4 space-y-3">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className={`flex items-end gap-2 animate-pulse ${
              i % 2 === 0 ? 'justify-end' : 'justify-start'
            }`}
          >
            {i % 2 !== 0 && <div className="w-8 h-8 rounded-full bg-slate-200 dark:bg-slate-800" />}
            <div
              className={`h-10 rounded-2xl bg-slate-200 dark:bg-slate-800 ${
                i % 2 === 0 ? 'w-48' : 'w-64'
              }`}
            />
          </div>
        ))}
      </div>
    );
  }

  if (allMessages.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-6 text-center text-slate-400">
        <div className="w-12 h-12 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-500 flex items-center justify-center mb-3">
          <MessageSquareDashed className="w-6 h-6" />
        </div>
        <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
          Chưa có tin nhắn nào
        </p>
        <p className="text-xs text-slate-400 mt-1 max-w-xs">
          Hãy gửi lời chào để bắt đầu cuộc trò chuyện thân mật ngay bây giờ!
        </p>
      </div>
    );
  }

  return (
    <div
      ref={scrollContainerRef}
      className="flex-1 overflow-y-auto px-4 py-3 space-y-1"
    >
      {/* Sentinel tải tin nhắn cũ */}
      <div ref={topSentinelRef} className="h-6 flex items-center justify-center">
        {isFetchingNextPage && (
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
            <span>Đang tải tin nhắn cũ hơn...</span>
          </div>
        )}
      </div>

      {/* Danh sách tin nhắn kèm phân cách ngày */}
      {allMessages.map((message, index) => {
        const isMe = message.senderId === currentUserId;
        const prevMessage = index > 0 ? allMessages[index - 1] : null;

        // Kiểm tra xem có cần hiển thị phân cách ngày không
        const currentDate = new Date(message.createdAt).toDateString();
        const prevDate = prevMessage ? new Date(prevMessage.createdAt).toDateString() : null;
        const showDateDivider = currentDate !== prevDate;

        // Trong group, chỉ hiển thị avatar và tên nếu tin trước đó không phải cùng người gửi
        const isSameSenderAsPrev = prevMessage?.senderId === message.senderId;
        const showAvatar = !isMe && (!isSameSenderAsPrev || showDateDivider);
        const showSenderName = isGroup && !isMe && (!isSameSenderAsPrev || showDateDivider);

        return (
          <React.Fragment key={message.messageId}>
            {showDateDivider && (
              <div className="flex items-center justify-center my-4 select-none">
                <span className="px-3 py-1 rounded-full bg-slate-100 dark:bg-slate-800/80 text-[11px] font-semibold text-slate-500 dark:text-slate-400 border border-slate-200/50 dark:border-slate-700/50 shadow-2xs">
                  {formatDateDivider(message.createdAt)}
                </span>
              </div>
            )}

            <MessageBubble
              message={message}
              isMe={isMe}
              showAvatar={showAvatar}
              showSenderName={showSenderName}
              onReply={onReplyMessage}
              onDelete={onDeleteMessage}
              onImageClick={onImageClick}
            />
          </React.Fragment>
        );
      })}

      <div ref={bottomRef} className="h-2" />
    </div>
  );
}
