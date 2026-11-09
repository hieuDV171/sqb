import { useRef } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { MessageSquare, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { CommentSection } from '@/features/feed/components/CommentSection';

export function SessionDiscussionPanel({ sessionId, title }: { sessionId: number; title: string }) {
  const closeButton = useRef<HTMLButtonElement>(null);

  return (
    <Dialog.Portal>
      <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/55" />
      <Dialog.Popup
        initialFocus={closeButton}
        className="fixed inset-x-0 bottom-0 z-50 flex h-[min(84dvh,44rem)] min-w-0 flex-col overflow-hidden rounded-t-2xl border border-border bg-card shadow-xl sm:inset-y-0 sm:left-auto sm:h-auto sm:w-full sm:max-w-lg sm:rounded-none"
      >
        <div className="flex items-start justify-between gap-3 border-b border-border p-4 sm:p-5">
          <div className="min-w-0 space-y-1.5">
            <p className="flex items-center gap-2 text-xs font-semibold text-academic">
              <MessageSquare aria-hidden="true" className="size-4" /> Luyện tập
            </p>
            <Dialog.Title className="text-xl font-semibold tracking-tight">Thảo luận bộ câu hỏi</Dialog.Title>
            <p className="wrap-anywhere text-sm font-medium">{title}</p>
            <Dialog.Description className="text-sm text-muted-foreground">
              Trao đổi về độ khó, nội dung hoặc cách ôn tập cho cả bộ câu hỏi.
            </Dialog.Description>
          </div>
          <Dialog.Close
            ref={closeButton}
            render={<Button variant="ghost" size="icon" className="size-11 shrink-0" />}
            aria-label="Đóng thảo luận"
          >
            <X aria-hidden="true" />
          </Dialog.Close>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-4 sm:p-5">
          <CommentSection targetType="SESSION" targetId={sessionId} />
        </div>
      </Dialog.Popup>
    </Dialog.Portal>
  );
}
