import React, { useState } from 'react';
import { X, AlertCircle, Loader2, Send } from 'lucide-react';

interface RejectQuestionModalProps {
  isOpen: boolean;
  questionId: number;
  questionNumber: number;
  isRejecting: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => Promise<void>;
}

export const RejectQuestionModal: React.FC<RejectQuestionModalProps> = ({
  isOpen,
  questionNumber,
  isRejecting,
  onClose,
  onConfirm,
}) => {
  const [reason, setReason] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onConfirm(reason.trim());
    setReason('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-md w-full p-5 space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2 text-rose-600 dark:text-rose-400">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <h3 className="font-bold text-slate-800 dark:text-slate-100 text-sm md:text-base">
              Từ chối Câu hỏi {questionNumber}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isRejecting}
            className="p-1 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="text-xs text-slate-600 dark:text-slate-400">
            Vui lòng nhập lý do từ chối để sinh viên hiểu rõ sai sót và cải thiện trong các lần đề xuất sau:
          </div>

          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Ví dụ: Đề bài chưa rõ ràng; Đáp án A và C trùng nhau; Trùng lặp với câu hỏi đã có trong ngân hàng..."
            rows={4}
            className="w-full bg-slate-50 dark:bg-slate-800/60 rounded-2xl p-3 text-xs md:text-sm text-slate-800 dark:text-slate-100 placeholder:text-slate-400 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-all resize-none"
          />

          {/* Actions */}
          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isRejecting}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isRejecting}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-rose-600 hover:bg-rose-700 shadow-md shadow-rose-500/20 transition-all cursor-pointer disabled:opacity-50"
            >
              {isRejecting ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <>
                  <span>Xác nhận từ chối</span>
                  <Send className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
