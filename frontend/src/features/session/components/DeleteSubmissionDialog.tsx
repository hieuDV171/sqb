import React from 'react';
import { AlertTriangle, Trash2, X } from 'lucide-react';

interface DeleteSubmissionDialogProps {
  isOpen: boolean;
  sessionId: number | null;
  sessionCode?: string;
  onClose: () => void;
  onConfirm: (sessionId: number) => Promise<any>;
  isDeleting: boolean;
}

export const DeleteSubmissionDialog: React.FC<DeleteSubmissionDialogProps> = ({
  isOpen,
  sessionId,
  sessionCode,
  onClose,
  onConfirm,
  isDeleting,
}) => {
  if (!isOpen || !sessionId) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="w-12 h-12 rounded-2xl bg-rose-100 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 flex items-center justify-center">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div>
          <h4 className="text-base font-bold text-slate-900 dark:text-white">
            Xác nhận xóa phiên đề xuất?
          </h4>
          <p className="text-xs text-slate-600 dark:text-slate-400 mt-1 leading-relaxed">
            Bạn có chắc chắn muốn xóa phiên đề xuất{' '}
            <strong className="font-mono text-slate-800 dark:text-slate-200">
              {sessionCode || `#${sessionId}`}
            </strong>
            ? Toàn bộ câu hỏi và hình ảnh đính kèm của phiên này sẽ bị xóa và không thể khôi phục.
          </p>
        </div>

        <div className="flex items-center justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            className="px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs sm:text-sm font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={async () => {
              await onConfirm(sessionId);
              onClose();
            }}
            disabled={isDeleting}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs sm:text-sm font-semibold shadow-xs transition-colors cursor-pointer disabled:opacity-50"
          >
            <Trash2 className="w-4 h-4" />
            <span>{isDeleting ? 'Đang xóa...' : 'Xóa vĩnh viễn'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
