import { AlertTriangle, LoaderCircle, RefreshCcw, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useModalFocusTrap } from '@/hooks/useModalFocusTrap';

interface AdminLeaderboardRebuildDialogProps {
  open: boolean;
  targetLabel: string;
  isPending: boolean;
  errorMessage?: string;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

export function AdminLeaderboardRebuildDialog({
  open,
  targetLabel,
  isPending,
  errorMessage,
  onClose,
  onConfirm,
}: AdminLeaderboardRebuildDialogProps) {
  const dialogRef = useModalFocusTrap(open, onClose, isPending);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/65 p-4 backdrop-blur-xs motion-safe:animate-in motion-safe:fade-in motion-safe:duration-150"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isPending) onClose();
      }}
    >
      <div
        ref={dialogRef}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="rebuild-dialog-title"
        aria-describedby="rebuild-dialog-description"
        aria-busy={isPending}
        className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-5 shadow-2xl dark:border-slate-800 dark:bg-slate-900 sm:p-6"
      >
        <div className="flex items-start gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-amber-100 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300">
            <AlertTriangle className="size-6" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 id="rebuild-dialog-title" className="text-lg font-semibold text-slate-950 dark:text-white">
              Xác nhận tái tạo bảng xếp hạng
            </h2>
            <p id="rebuild-dialog-description" className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-300">
              Hệ thống sẽ thay toàn bộ cache Redis của <strong>{targetLabel}</strong> bằng dữ liệu tổng hợp lại từ PostgreSQL.
              Trong lúc xử lý, kết quả xếp hạng có thể thay đổi.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isPending}
            aria-label="Đóng hộp thoại xác nhận"
            className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-800 dark:hover:text-slate-100"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <div className="mt-5 rounded-xl border border-sky-200 bg-sky-50 p-3.5 text-sm leading-relaxed text-sky-900 dark:border-sky-900/60 dark:bg-sky-950/40 dark:text-sky-200">
          Dữ liệu điểm gốc không bị chỉnh sửa. Thao tác này chỉ đồng bộ lại lớp dữ liệu đọc nhanh trong Redis.
        </div>

        {errorMessage && (
          <div role="alert" className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-800 dark:border-rose-900/60 dark:bg-rose-950/40 dark:text-rose-200">
            {errorMessage} Bạn có thể thử lại hoặc đóng hộp thoại để đổi phạm vi.
          </div>
        )}

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isPending}
            data-autofocus
            className="min-h-11 cursor-pointer rounded-xl px-5"
          >
            Hủy bỏ
          </Button>
          <Button
            type="button"
            onClick={() => void onConfirm()}
            disabled={isPending}
            className="min-h-11 cursor-pointer rounded-xl bg-indigo-600 px-5 text-white hover:bg-indigo-700"
          >
            {isPending ? (
              <LoaderCircle className="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />
            ) : (
              <RefreshCcw className="size-4" aria-hidden="true" />
            )}
            {isPending ? 'Đang tái tạo…' : 'Xác nhận tái tạo'}
          </Button>
        </div>
      </div>
    </div>
  );
}
