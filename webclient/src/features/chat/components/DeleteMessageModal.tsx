import { useState } from 'react';
import { useDeleteMessage } from '../hooks/useChat';
import { useAuthStore } from '@/stores/useAuthStore';
import type { MessageDto, MessageDeleteScope } from '../types/chat.types';
import { Trash2, X, Loader2 } from 'lucide-react';

interface DeleteMessageModalProps {
  isOpen: boolean;
  onClose: () => void;
  message: MessageDto | null;
  conversationId: number;
}

export function DeleteMessageModal({
  isOpen,
  onClose,
  message,
  conversationId,
}: DeleteMessageModalProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [scope, setScope] = useState<MessageDeleteScope>('EVERYONE');

  const deleteMessageMutation = useDeleteMessage();

  if (!isOpen || !message) return null;

  const isSender = message.senderId === currentUserId;

  const handleDelete = () => {
    deleteMessageMutation.mutate(
      {
        conversationId,
        messageId: message.messageId,
        data: {
          scope: isSender ? scope : 'ME',
        },
      },
      {
        onSuccess: () => {
          onClose();
        },
      }
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in">
      <div
        className="w-full max-w-sm bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl p-6 relative animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex flex-col items-center text-center mb-5">
          <div className="w-12 h-12 rounded-2xl bg-red-500/10 text-red-500 flex items-center justify-center mb-3">
            <Trash2 className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
            Xác nhận xóa tin nhắn
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Chọn hình thức xóa phù hợp với mong muốn của bạn.
          </p>
        </div>

        {/* Options */}
        <div className="space-y-2 mb-6">
          {isSender && (
            <label
              onClick={() => setScope('EVERYONE')}
              className={`flex items-start gap-3 p-3 rounded-2xl border cursor-pointer transition-all ${
                scope === 'EVERYONE'
                  ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                  : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50'
              }`}
            >
              <input
                type="radio"
                name="scope"
                checked={scope === 'EVERYONE'}
                onChange={() => setScope('EVERYONE')}
                className="mt-1 text-indigo-600 focus:ring-indigo-500"
              />
              <div className="text-left">
                <span className="block text-xs font-bold text-slate-900 dark:text-slate-100">
                  Thu hồi với mọi người
                </span>
                <span className="block text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                  Tin nhắn sẽ bị thu hồi đối với tất cả thành viên trong cuộc hội thoại.
                </span>
              </div>
            </label>
          )}

          <label
            onClick={() => setScope('ME')}
            className={`flex items-start gap-3 p-3 rounded-2xl border cursor-pointer transition-all ${
              scope === 'ME' || !isSender
                ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50'
            }`}
          >
            <input
              type="radio"
              name="scope"
              checked={scope === 'ME' || !isSender}
              onChange={() => setScope('ME')}
              className="mt-1 text-indigo-600 focus:ring-indigo-500"
            />
            <div className="text-left">
              <span className="block text-xs font-bold text-slate-900 dark:text-slate-100">
                Xóa ở phía tôi
              </span>
              <span className="block text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                Chỉ ẩn tin nhắn trên thiết bị của bạn. Những người khác vẫn xem được bình thường.
              </span>
            </div>
          </label>
        </div>

        {/* Buttons */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold text-xs hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleDelete}
            disabled={deleteMessageMutation.isPending}
            className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-semibold text-xs shadow-md transition-colors cursor-pointer flex items-center justify-center gap-1.5"
          >
            {deleteMessageMutation.isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Xác nhận
          </button>
        </div>
      </div>
    </div>
  );
}
