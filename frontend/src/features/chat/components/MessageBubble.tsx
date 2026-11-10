import { getAvatarInitial } from '@/lib/avatar';
import type { MessageDto } from '../types/chat.types';
import { Reply, Trash2, Check, Ban, CornerDownRight } from 'lucide-react';

interface MessageBubbleProps {
  message: MessageDto;
  isMe: boolean;
  showAvatar?: boolean;
  showSenderName?: boolean;
  onReply: (message: MessageDto) => void;
  onJumpToOriginal: (messageId: number) => void;
  onDelete: (message: MessageDto) => void;
  onImageClick?: (url: string) => void;
}

export function MessageBubble({
  message,
  isMe,
  showAvatar = true,
  showSenderName = false,
  onReply,
  onJumpToOriginal,
  onDelete,
  onImageClick,
}: MessageBubbleProps) {
  const timeFormatted = new Date(message.createdAt).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  const quote = message.replyToMessage;

  if (message.revoked) {
    return (
      <div className={`flex w-full ${isMe ? 'justify-end' : 'justify-start'} my-1`}>
        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-2xl bg-slate-100/70 dark:bg-slate-800/50 border border-slate-200/60 dark:border-slate-800 text-slate-400 dark:text-slate-500 text-xs italic select-none">
          <Ban className="w-3.5 h-3.5" />
          <span>Tin nhắn đã được thu hồi</span>
          <span className="text-[10px] ml-1 text-slate-400/80 not-italic">{timeFormatted}</span>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`group relative flex w-full gap-2 my-1.5 items-end ${
        isMe ? 'justify-end' : 'justify-start'
      }`}
    >
      {/* Sender Avatar (For Others) */}
      {!isMe && (
        <div className="w-8 h-8 shrink-0">
          {showAvatar ? (
            <div className="w-8 h-8 rounded-full overflow-hidden bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-semibold shadow-2xs">
              {message.avatarUrl ? (
                <img src={message.avatarUrl} alt={message.senderName} className="w-full h-full object-cover" />
              ) : (
                getAvatarInitial(message.senderName)
              )}
            </div>
          ) : (
            <div className="w-8 h-8" />
          )}
        </div>
      )}

      {/* Bubble Container */}
      <div className={`min-w-0 max-w-[78%] sm:max-w-[70%] flex flex-col ${isMe ? 'items-end' : 'items-start'}`}>
        {/* Sender Name in Group */}
        {!isMe && showSenderName && (
          <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1 ml-1">
            {message.senderName}
          </span>
        )}

        {/* Quoted Message Preview */}
        {quote && (quote.unavailable ? (
          <div className="mb-1 max-w-full rounded-xl border-l-2 border-border bg-muted px-3 py-2 text-xs text-muted-foreground">
            {quote.contentPreview}
          </div>
        ) : (
          <button
            type="button"
            onClick={() => onJumpToOriginal(quote.messageId)}
            aria-label={`Đến tin nhắn gốc${quote.senderName ? ` của ${quote.senderName}` : ''}: ${quote.contentPreview}`}
            className="mb-1 flex min-h-11 max-w-full cursor-pointer items-center gap-2 rounded-xl border-l-2 border-primary bg-muted px-3 py-1.5 text-left text-xs hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring"
          >
            <CornerDownRight aria-hidden="true" className="size-3.5 shrink-0 text-muted-foreground" />
            <span className="min-w-0">
              {quote.senderName && <span className="block truncate font-semibold text-foreground">{quote.senderName}</span>}
              <span className="block truncate text-muted-foreground">{quote.contentPreview}</span>
            </span>
          </button>
        ))}

        {/* Message Main Bubble */}
        <div
          className={`relative px-4 py-2.5 rounded-2xl shadow-xs text-sm leading-relaxed break-words ${
            isMe
              ? 'bg-linear-to-r from-indigo-600 to-violet-600 text-white rounded-br-xs'
              : 'bg-card text-foreground border border-slate-200/70 dark:border-slate-700/60 rounded-bl-xs'
          }`}
        >
          {/* Images Grid */}
          {message.mediaUrls && message.mediaUrls.length > 0 && (
            <div
              className={`grid gap-1.5 mb-2 rounded-xl overflow-hidden ${
                message.mediaUrls.length === 1
                  ? 'grid-cols-1'
                  : message.mediaUrls.length === 2
                  ? 'grid-cols-2'
                  : 'grid-cols-3'
              }`}
            >
              {message.mediaUrls.map((url, idx) => (
                <img
                  key={idx}
                  src={url}
                  alt="Ảnh đính kèm"
                  onClick={() => onImageClick?.(url)}
                  className="w-full max-h-64 object-cover rounded-lg cursor-pointer hover:opacity-95 transition-opacity"
                  loading="lazy"
                />
              ))}
            </div>
          )}

          {/* Text Content */}
          {message.content && (
            <p className="whitespace-pre-wrap selection:bg-indigo-300 selection:text-indigo-900">
              {message.content}
            </p>
          )}

          {/* Footer inside Bubble: Timestamp & Status */}
          <div
            className={`flex items-center justify-end gap-1 mt-1 text-[10px] select-none ${
              isMe ? 'text-indigo-200' : 'text-slate-400 dark:text-slate-500'
            }`}
          >
            <span>{timeFormatted}</span>
            {isMe && (
              <span title="Đã gửi">
                <Check className="w-3 h-3" />
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Floating Action Menu (Reply, Delete) */}
      <div
        className={`flex shrink-0 items-center gap-0.5 transition-opacity sm:opacity-0 sm:group-hover:opacity-100 sm:group-focus-within:opacity-100 pointer-coarse:opacity-100 ${
          isMe ? 'order-first mr-1' : 'ml-1'
        }`}
      >
        <button
          type="button"
          onClick={() => onReply(message)}
          title="Trả lời tin nhắn này" aria-label="Trả lời tin nhắn"
          className="grid size-9 place-items-center rounded-lg text-muted-foreground hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <Reply className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onClick={() => onDelete(message)}
          aria-label={isMe ? 'Thu hồi tin nhắn' : 'Xóa tin nhắn phía bạn'} title={isMe ? 'Thu hồi tin nhắn' : 'Xóa tin nhắn phía bạn'}
          className="grid size-9 place-items-center rounded-lg text-muted-foreground hover:text-red-600 dark:hover:text-red-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
