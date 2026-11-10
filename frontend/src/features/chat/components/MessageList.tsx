import React, { useCallback, useEffect, useLayoutEffect, useRef, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
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
  const topSentinelRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const [jump, setJump] = useState<{ messageId: number } | null>(null);
  const focusedJump = useRef<typeof jump>(null);

  const {
    data,
    isLoading,
    isLoadingError,
    isFetchNextPageError,
    isFetching,
    refetch,
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

  const nearBottom = useRef(true);
  const prependAnchor = useRef<{ height: number; top: number; pages: number } | null>(null);
  const pageCount = data?.pages.length ?? 0;
  const lastMessageId = allMessages.at(-1)?.messageId;
  const oldestCursor = data?.pages.at(-1)?.pagination.after;
  const targetLoaded = jump !== null && allMessages.some((message) => message.messageId === jump.messageId);
  const targetMayBeOlder = jump !== null && hasNextPage && oldestCursor != null && oldestCursor > jump.messageId;
  const jumpPending = jump !== null && !targetLoaded && (isFetching || (targetMayBeOlder && !isFetchNextPageError));
  let jumpNotice: string | null = null;
  if (jump && !targetLoaded) {
    if (jumpPending) {
      jumpNotice = 'Đang tìm tin nhắn gốc…';
    } else if (isFetchNextPageError && targetMayBeOlder) {
      jumpNotice = 'Không tải được tin nhắn gốc. Bấm lại ô trích dẫn để thử.';
    } else {
      jumpNotice = 'Tin nhắn gốc không còn hiển thị trong cuộc trò chuyện.';
    }
  }

  useLayoutEffect(() => {
    const container = scrollContainerRef.current;
    const anchor = prependAnchor.current;
    if (!container || !anchor || pageCount <= anchor.pages) return;
    container.scrollTop = anchor.top + container.scrollHeight - anchor.height;
    prependAnchor.current = null;
  }, [pageCount]);

  useLayoutEffect(() => {
    const container = scrollContainerRef.current;
    if (container && nearBottom.current) container.scrollTop = container.scrollHeight;
  }, [conversationId, lastMessageId]);

  useLayoutEffect(() => {
    if (!jump || !targetLoaded || focusedJump.current === jump) return;
    const container = scrollContainerRef.current;
    const target = container?.querySelector<HTMLElement>(`[data-message-id="${jump.messageId}"]`);
    if (!container || !target) return;
    focusedJump.current = jump;
    const targetRect = target.getBoundingClientRect();
    const viewportRect = container.getBoundingClientRect();
    container.scrollTop += targetRect.top - viewportRect.top - (container.clientHeight - targetRect.height) / 2;
    nearBottom.current = container.scrollHeight - container.scrollTop - container.clientHeight < 80;
    target.focus({ preventScroll: true });
  }, [jump, targetLoaded]);

  useEffect(() => {
    if (!jump || jumpPending) return;
    const timeout = window.setTimeout(() => setJump((current) => current === jump ? null : current), targetLoaded ? 2200 : 6500);
    return () => window.clearTimeout(timeout);
  }, [jump, jumpPending, targetLoaded]);

  const loadOlder = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container || !hasNextPage || isFetching) return;
    prependAnchor.current = { height: container.scrollHeight, top: container.scrollTop, pages: pageCount };
    void fetchNextPage({ cancelRefetch: false });
  }, [fetchNextPage, hasNextPage, isFetching, pageCount]);

  function jumpToOriginal(messageId: number) {
    setJump({ messageId });
    if (isFetchNextPageError && oldestCursor != null && oldestCursor > messageId
        && !allMessages.some((message) => message.messageId === messageId)) {
      loadOlder();
    }
  }

  useEffect(() => {
    const root = scrollContainerRef.current;
    const sentinel = topSentinelRef.current;
    if (jumpPending) {
      loadOlder();
      return;
    }
    if (!root || !sentinel || !hasNextPage || isFetching || isFetchNextPageError || typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver(([entry]) => {
      if (!entry?.isIntersecting) return;
      observer.disconnect();
      loadOlder();
    }, { root, rootMargin: '100px 0px 0px 0px' });
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNextPage, isFetching, isFetchNextPageError, jumpPending, loadOlder]);

  if (isLoadingError) {
    return <div className="min-h-0 flex-1 overflow-y-auto p-3"><ErrorState title="Không thể tải tin nhắn" onRetry={() => void refetch()} /></div>;
  }

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
            {i % 2 !== 0 && <div className="w-8 h-8 rounded-full bg-muted" />}
            <div
              className={`h-10 rounded-2xl bg-muted ${
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
      <div className="flex-1 flex flex-col items-center justify-center p-6 text-center text-muted-foreground">
        <div className="w-12 h-12 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-500 flex items-center justify-center mb-3">
          <MessageSquareDashed className="w-6 h-6" />
        </div>
        <p className="text-sm font-semibold text-foreground">
          Chưa có tin nhắn nào
        </p>
        <p className="text-xs text-muted-foreground mt-1 max-w-xs">
          Hãy gửi lời chào để bắt đầu cuộc trò chuyện thân mật ngay bây giờ!
        </p>
      </div>
    );
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div
        ref={scrollContainerRef}
        className="min-h-0 flex-1 space-y-2 overflow-y-auto overscroll-contain px-2 py-3 sm:px-4 [overflow-anchor:none]"
        onScroll={(event) => {
          const element = event.currentTarget;
          nearBottom.current = element.scrollHeight - element.scrollTop - element.clientHeight < 80;
        }}
      >
        {/* Sentinel tải tin nhắn cũ */}
        <div ref={topSentinelRef} className="flex min-h-2 items-center justify-center">
          {hasNextPage && !isFetchingNextPage && (isFetchNextPageError || typeof IntersectionObserver === 'undefined') && (
            <Button variant="ghost" className="min-h-11" onClick={loadOlder}>
              {isFetchNextPageError ? 'Thử tải lại tin nhắn cũ' : 'Tải tin nhắn cũ'}
            </Button>
          )}
          {isFetchingNextPage && (
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Loader2 className="w-3.5 h-3.5 motion-safe:animate-spin" />
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
                  <span className="px-3 py-1 rounded-full bg-slate-100 dark:bg-slate-800/80 text-[11px] font-semibold text-slate-500 dark:text-muted-foreground border border-slate-200/50 dark:border-slate-700/50 shadow-2xs">
                    {formatDateDivider(message.createdAt)}
                  </span>
                </div>
              )}

              <div
                data-message-id={message.messageId}
                tabIndex={-1}
                className={`flow-root rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring ${
                  jump?.messageId === message.messageId ? 'bg-accent ring-2 ring-inset ring-primary/60' : ''
                }`}
              >
                <MessageBubble
                  message={message}
                  isMe={isMe}
                  showAvatar={showAvatar}
                  showSenderName={showSenderName}
                  onReply={onReplyMessage}
                  onJumpToOriginal={jumpToOriginal}
                  onDelete={onDeleteMessage}
                  onImageClick={onImageClick}
                />
              </div>
            </React.Fragment>
          );
        })}

      </div>
      <div role="status" className={jumpNotice ? 'shrink-0 border-t border-border px-3 py-2 text-xs text-muted-foreground' : 'sr-only'}>
        {jumpNotice}
      </div>
    </div>
  );
}
