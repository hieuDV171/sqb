import { AlertDialog } from '@base-ui/react/alert-dialog';
import { useState } from 'react';
import { ActionError } from '@/components/ui/action-error';
import { getActionErrorMessage } from '@/lib/errorMessage';
import { Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface DeleteSubmissionDialogProps {
  isOpen: boolean;
  sessionId: number | null;
  sessionCode?: string;
  onClose: () => void;
  onConfirm: (sessionId: number) => Promise<void>;
  isDeleting: boolean;
}

export function DeleteSubmissionDialog({ isOpen, sessionId, ...props }: DeleteSubmissionDialogProps) {
  if (!isOpen || !sessionId) return null;
  return <DeleteSubmissionConfirmation key={sessionId} sessionId={sessionId} {...props} />;
}

function DeleteSubmissionConfirmation({
  sessionId, sessionCode, onClose, onConfirm, isDeleting,
}: Omit<DeleteSubmissionDialogProps, 'isOpen' | 'sessionId'> & { sessionId: number }) {
  const [error, setError] = useState<string>();

  return (
    <AlertDialog.Root open onOpenChange={(open) => { if (!open && !isDeleting) onClose(); }}>
      <AlertDialog.Portal>
        <AlertDialog.Backdrop className="fixed inset-0 z-60 bg-black/60" />
        <AlertDialog.Popup className="fixed inset-x-4 top-1/2 z-60 mx-auto max-w-md -translate-y-1/2 space-y-4 rounded-2xl border border-border bg-card p-5 shadow-xl">
          <AlertDialog.Title className="text-lg font-semibold">Xóa phiên đề xuất?</AlertDialog.Title>
          <AlertDialog.Description className="text-sm leading-relaxed text-muted-foreground">
            Phiên <strong className="font-mono text-foreground">{sessionCode || `#${sessionId}`}</strong> cùng toàn bộ câu hỏi và hình ảnh đính kèm sẽ bị xóa vĩnh viễn.
          </AlertDialog.Description>
          <ActionError message={error} />
          <div className="flex flex-wrap justify-end gap-2">
            <AlertDialog.Close render={<Button variant="outline" className="min-h-11" disabled={isDeleting} />}>Hủy</AlertDialog.Close>
            <Button variant="destructive" className="min-h-11" disabled={isDeleting} onClick={async () => {
              setError(undefined);
              try {
                await onConfirm(sessionId);
                onClose();
              } catch (failure) {
                setError(getActionErrorMessage(failure, 'Không thể xóa phiên đề xuất. Hãy thử lại.'));
              }
            }}>
              <Trash2 aria-hidden="true" /> {isDeleting ? 'Đang xóa…' : 'Xóa vĩnh viễn'}
            </Button>
          </div>
        </AlertDialog.Popup>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
