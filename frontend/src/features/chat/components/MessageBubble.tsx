import type { MessageDto } from '../types/chat.types';
import { Reply, Trash2, Check, Ban, CornerDownRight } from 'lucide-react';

interface MessageBubbleProps {
  message: MessageDto;
  isMe: boolean;
  showAvatar?: boolean;
  showSenderName?: boolean;
  onReply: (message: MessageDto) => void;
  onDelete: (message: MessageDto) => void;
  onImageClick?: (url: string) => void;
}

export function MessageBubble({
  message,
  isMe,
  showAvatar = true,
  showSenderName = false,
  onReply,
  onDelete,
  onImageClick,
}: MessageBubbleProps) {
  const timeFormatted = new Date(message.createdAt).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

  if (message.isRevoked) {
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
                message.senderName?.charAt(0).toUpperCase() || 'U'
              )}
            </div>
          ) : (
            <div className="w-8 h-8" />
          )}
        </div>
      )}

      {/* Bubble Container */}
      <div className={`max-w-[78%] sm:max-w-[70%] flex flex-col ${isMe ? 'items-end' : 'items-start'}`}>
        {/* Sender Name in Group */}
        {!isMe && showSenderName && (
          <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1 ml-1">
            {message.senderName}
          </span>
        )}

        {/* Quoted Message Preview */}
        {message.replyToMessage && (
          <div
            className={`flex items-start gap-1.5 text-xs px-3 py-1.5 mb-1 rounded-xl border-l-2 bg-slate-100/80 dark:bg-slate-800/80 ${
              isMe
                ? 'border-indigo-400 text-slate-600 dark:text-slate-300'
                : 'border-slate-400 text-slate-600 dark:text-slate-300'
            }`}
          >
            <CornerDownRight className="w-3.5 h-3.5 mt-0.5 text-slate-400 shrink-0" />
            <div className="truncate">
              <span className="font-semibold">{message.replyToMessage.senderName}: </span>
              <span>
                {message.replyToMessage.messageType === 'IMAGE'
                  ? '[Hình ảnh]'
                  : message.replyToMessage.content}
              </span>
            </div>
          </div>
        )}

        {/* Message Main Bubble */}
        <div
          className={`relative px-4 py-2.5 rounded-2xl shadow-xs text-sm leading-relaxed break-words ${
            isMe
              ? 'bg-linear-to-r from-indigo-600 to-violet-600 text-white rounded-br-xs'
              : 'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 border border-slate-200/70 dark:border-slate-700/60 rounded-bl-xs'
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
        className={`opacity-0 group-hover:opacity-100 flex items-center gap-1 transition-opacity ${
          isMe ? 'order-first mr-1' : 'ml-1'
        }`}
      >
        <button
          type="button"
          onClick={() => onReply(message)}
          title="Trả lời tin nhắn này"
          className="p-1 rounded-lg text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <Reply className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onClick={() => onDelete(message)}
          title={isMe ? 'Thu hồi tin nhắn' : 'Xóa tin nhắn phía bạn'}
          className="p-1 rounded-lg text-slate-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
