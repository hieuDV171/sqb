import React from 'react';
import { Award, AlertTriangle, Loader2, CheckCircle2, X } from 'lucide-react';

interface CompleteReviewDialogProps {
  isOpen: boolean;
  sessionId: number;
  approvedCount: number;
  pendingCount: number;
  rejectedCount: number;
  isCompleting: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

export const CompleteReviewDialog: React.FC<CompleteReviewDialogProps> = ({
  isOpen,
  sessionId,
  approvedCount,
  pendingCount,
  rejectedCount,
  isCompleting,
  onClose,
  onConfirm,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-md w-full p-5 space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
            <Award className="w-5 h-5 shrink-0" />
            <h3 className="font-bold text-slate-800 dark:text-slate-100 text-sm md:text-base">
              Hoàn tất Đánh giá Phiên #{sessionId}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isCompleting}
            className="p-1 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content & Stats */}
        <div className="space-y-3">
          <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
            Bạn đang chuẩn bị chốt duyệt toàn bộ phiên đề xuất này. Tổng kết đánh giá:
          </p>

          <div className="grid grid-cols-3 gap-2 py-2">
            <div className="p-2.5 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200/60 dark:border-emerald-800 text-center">
              <span className="block text-lg font-bold text-emerald-600 dark:text-emerald-400">
                {approvedCount}
              </span>
              <span className="text-[11px] font-semibold text-emerald-800 dark:text-emerald-300">
                Đã duyệt
              </span>
            </div>

            <div className="p-2.5 rounded-2xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200/60 dark:border-amber-800 text-center">
              <span className="block text-lg font-bold text-amber-600 dark:text-amber-400">
                {pendingCount}
              </span>
              <span className="text-[11px] font-semibold text-amber-800 dark:text-amber-300">
                Chưa duyệt
              </span>
            </div>

            <div className="p-2.5 rounded-2xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200/60 dark:border-rose-800 text-center">
              <span className="block text-lg font-bold text-rose-600 dark:text-rose-400">
                {rejectedCount}
              </span>
              <span className="text-[11px] font-semibold text-rose-800 dark:text-rose-300">
                Đã từ chối
              </span>
            </div>
          </div>

          {pendingCount > 0 && (
            <div className="flex items-start gap-2 p-3 rounded-2xl bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200 text-xs">
              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <span>
                <strong>Lưu ý:</strong> Có <strong>{pendingCount}</strong> câu hỏi vẫn đang ở trạng thái chờ. Khi hoàn tất, các câu này sẽ tự động chuyển thành <strong>Từ chối (REJECTED)</strong>!
              </span>
            </div>
          )}

          <div className="flex items-start gap-2 p-3 rounded-2xl bg-blue-50 dark:bg-blue-950/50 border border-blue-200 dark:border-blue-800 text-blue-900 dark:text-blue-200 text-xs">
            <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
            <span>
              Hệ thống sẽ chuyển phiên sang trạng thái <strong>RESOLVED</strong>, tự động cộng điểm thưởng cho sinh viên đề xuất và gửi thông báo vinh danh.
            </span>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            disabled={isCompleting}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Xem lại
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isCompleting}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-md shadow-indigo-500/20 transition-all cursor-pointer disabled:opacity-50"
          >
            {isCompleting ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang hoàn tất...</span>
              </>
            ) : (
              <>
                <Award className="w-3.5 h-3.5" />
                <span>Chốt hoàn tất duyệt</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
