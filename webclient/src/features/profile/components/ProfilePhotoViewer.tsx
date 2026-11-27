import { useRef, useState, type ReactNode } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { ImageOff, Loader2, RotateCw, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface ProfilePhotoViewerProps {
  url?: string | null;
  label: string;
  initialAspectRatio?: number;
  className?: string;
  children: ReactNode;
}

export function ProfilePhotoViewer({ url, label, initialAspectRatio = 1, className, children }: ProfilePhotoViewerProps) {
  const closeButton = useRef<HTMLButtonElement>(null);

  if (!url?.trim()) return <div className={className}>{children}</div>;

  return (
    <Dialog.Root>
      <Dialog.Trigger
        aria-label={`Xem ${label}`}
        title={`Xem ${label}`}
        className={cn('block cursor-zoom-in focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring', className)}
      >
        {children}
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/75" />
        <Dialog.Popup
          initialFocus={closeButton}
          className="fixed inset-x-3 top-1/2 z-50 mx-auto flex max-h-[calc(100dvh-1.5rem)] max-w-5xl -translate-y-1/2 flex-col overflow-y-auto rounded-2xl border border-border bg-card p-3 text-card-foreground shadow-xl sm:p-4"
        >
          <div className="mb-3 flex shrink-0 items-center justify-between gap-3">
            <Dialog.Title className="min-w-0 wrap-anywhere text-base font-semibold sm:text-lg">{label}</Dialog.Title>
            <Dialog.Close ref={closeButton} render={<Button variant="ghost" size="icon" className="size-11 shrink-0" />} aria-label="Đóng ảnh">
              <X aria-hidden="true" />
            </Dialog.Close>
          </div>
          <Dialog.Description className="sr-only">Xem toàn bộ ảnh hiện tại trên hồ sơ. Nhấn Escape hoặc nút đóng để quay lại.</Dialog.Description>
          <PhotoContent key={url} url={url} label={label} initialAspectRatio={initialAspectRatio} />
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function PhotoContent({ url, label, initialAspectRatio }: { url: string; label: string; initialAspectRatio: number }) {
  const [status, setStatus] = useState<'loading' | 'loaded' | 'error'>('loading');
  const [attempt, setAttempt] = useState(0);
  const [aspectRatio, setAspectRatio] = useState(initialAspectRatio);

  return (
    <div
      className={cn('relative grid min-h-0 w-full place-items-center rounded-xl bg-surface-subtle', status === 'error' && 'overflow-y-auto')}
      style={{ aspectRatio: status === 'error' ? undefined : aspectRatio }}
      aria-busy={status === 'loading'}
    >
      {status === 'loading' && (
        <p role="status" className="absolute flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 aria-hidden="true" className="size-5 motion-safe:animate-spin" /> Đang tải ảnh…
        </p>
      )}
      {status === 'error' ? (
        <div className="space-y-3 p-4 text-center">
          <ImageOff aria-hidden="true" className="mx-auto size-8 text-muted-foreground" />
          <p role="alert" className="text-sm text-muted-foreground">Không thể tải ảnh. Vui lòng thử lại.</p>
          <Button variant="outline" className="min-h-11" onClick={() => { setStatus('loading'); setAttempt(attempt + 1); }}>
            <RotateCw aria-hidden="true" /> Thử lại
          </Button>
        </div>
      ) : (
        <img key={attempt} src={url} alt={label}
          className={cn('absolute inset-0 size-full object-contain', status === 'loading' && 'invisible')}
          onLoad={(event) => {
            const image = event.currentTarget;
            setAspectRatio(image.naturalWidth / image.naturalHeight);
            setStatus('loaded');
          }} onError={() => setStatus('error')} />
      )}
    </div>
  );
}
