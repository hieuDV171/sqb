import type { ReactNode } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { X, type LucideIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface ExamDialogProps {
  title: string;
  description: string;
  icon: LucideIcon;
  busy?: boolean;
  className?: string;
  onClose: () => void;
  children: ReactNode;
}

export function ExamDialog({ title, description, icon: Icon, busy, className, onClose, children }: ExamDialogProps) {
  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !busy) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className={cn(
          'fixed inset-x-3 top-1/2 z-50 mx-auto flex max-h-[calc(100dvh-1.5rem)] max-w-2xl -translate-y-1/2 flex-col overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-xl sm:inset-x-6',
          className,
        )}>
          <header className="flex shrink-0 items-start gap-3 border-b border-border p-4 sm:px-5">
            <span className="hidden size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic sm:grid">
              <Icon aria-hidden="true" className="size-5" />
            </span>
            <div className="min-w-0 grow">
              <Dialog.Title className="wrap-break-word text-xl font-semibold">{title}</Dialog.Title>
              <Dialog.Description className="mt-1 wrap-break-word text-sm text-muted-foreground">{description}</Dialog.Description>
            </div>
            <Dialog.Close render={<Button variant="ghost" size="icon" aria-label="Đóng hộp thoại" disabled={busy} />}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </header>
          {children}
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
