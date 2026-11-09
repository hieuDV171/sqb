import { useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { usePostMutations } from '../hooks/usePostMutations';

interface LecturerNoteModalProps {
  postId: number;
  initialNote?: string;
  onClose: () => void;
}

export function LecturerNoteModal({ postId, initialNote = '', onClose }: LecturerNoteModalProps) {
  const [note, setNote] = useState(initialNote);
  const { updateLecturerNote, isUpdatingNote } = usePostMutations();

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const content = note.trim();
    if (!content || isUpdatingNote) return;

    try {
      await updateLecturerNote({ id: postId, data: { content } });
      onClose();
    } catch {
      // The mutation displays the error. Keep the draft open so the lecturer can retry.
    }
  };

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !isUpdatingNote) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-3 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-lg -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl sm:p-6">
          <Dialog.Title className="text-lg font-semibold">Ghi chú của giảng viên</Dialog.Title>
          <Dialog.Description className="mt-1 text-sm text-muted-foreground">
            Ghi chú sẽ hiển thị công khai trên bài vinh danh phiên đề xuất.
          </Dialog.Description>
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <label className="block space-y-2 text-sm font-medium">
              <span>Nội dung ghi chú</span>
              <textarea
                value={note}
                onChange={(event) => setNote(event.target.value)}
                rows={4}
                required
                disabled={isUpdatingNote}
                className="block w-full rounded-xl border border-input bg-background p-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </label>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" disabled={isUpdatingNote} onClick={onClose} className="min-h-11">
                Hủy
              </Button>
              <Button type="submit" disabled={!note.trim() || isUpdatingNote} className="min-h-11">
                {isUpdatingNote && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
                {isUpdatingNote ? 'Đang lưu...' : 'Lưu ghi chú'}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
