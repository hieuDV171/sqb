import { AlertDialog } from '@base-ui/react/alert-dialog';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface CompleteReviewDialogProps {
  mode: 'approve' | 'complete';
  pendingCount: number;
  isSubmitting: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function CompleteReviewDialog({ mode, pendingCount, isSubmitting, onClose, onConfirm }: CompleteReviewDialogProps) {
  const approve = mode === 'approve';
  return (
    <AlertDialog.Root open onOpenChange={(open) => { if (!open && !isSubmitting) onClose(); }}>
      <AlertDialog.Portal>
        <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <AlertDialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <AlertDialog.Title className="text-xl font-semibold">
            {approve ? 'Duyệt toàn bộ câu còn chờ?' : 'Chốt phiên duyệt?'}
          </AlertDialog.Title>
          <AlertDialog.Description className="text-sm leading-relaxed text-muted-foreground">
            {approve
              ? `${pendingCount} câu còn chờ sẽ được phê duyệt và đưa vào ngân hàng câu hỏi. Hãy kiểm tra các cảnh báo tương đồng trước khi xác nhận.`
              : `${pendingCount} câu còn chờ sẽ tự động bị từ chối. Backend sẽ dùng số câu đã duyệt để tính điểm thưởng cho sinh viên.`}
          </AlertDialog.Description>
          <div className="flex flex-wrap justify-end gap-2">
            <AlertDialog.Close render={<Button variant="outline" className="min-h-11" disabled={isSubmitting} />}>Xem lại</AlertDialog.Close>
            <Button variant={approve ? 'default' : 'destructive'} className="min-h-11"
              onClick={onConfirm} disabled={isSubmitting || pendingCount === 0}>
              {isSubmitting && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
              {isSubmitting ? 'Đang lưu…' : approve ? `Duyệt ${pendingCount} câu` : 'Xác nhận chốt phiên'}
            </Button>
          </div>
        </AlertDialog.Popup>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
