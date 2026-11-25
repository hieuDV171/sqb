import { useSyncExternalStore } from 'react';
import { WifiOff } from 'lucide-react';
import { checkApiConnection, getConnectionSnapshot, subscribeToConnection } from '@/api/connectivity';
import { Button } from '@/components/ui/button';

export function ConnectionStatusPopup() {
  const { status, checking } = useSyncExternalStore(subscribeToConnection, getConnectionSnapshot);
  const message = status === 'offline' ? 'Bạn đang ngoại tuyến'
    : status === 'unavailable' ? 'Không thể kết nối tới SQB' : '';

  return (
    <div className="fixed z-30 w-max md:transition-[left] md:duration-200 motion-reduce:transition-none"
      data-slot="connection-status" data-connection-status={status}>
      <div className={message
        ? 'flex items-center gap-3 rounded-2xl border border-border bg-card px-3 py-2 text-sm text-card-foreground shadow-lg'
        : undefined}>
        <p role="status" aria-atomic="true" className="flex min-w-0 items-center gap-2.5 font-semibold">
          {message && (
            <>
              <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-warning-soft text-warning">
                <WifiOff aria-hidden="true" className="size-4" />
              </span>
              <span className="min-w-0">{message}</span>
            </>
          )}
        </p>
        {status === 'unavailable' && (
          <Button type="button" variant="ghost" className="min-h-11 px-2 underline" aria-disabled={checking}
            onClick={() => void checkApiConnection()}>
            {checking ? 'Đang kiểm tra…' : 'Thử lại'}
          </Button>
        )}
      </div>
    </div>
  );
}
