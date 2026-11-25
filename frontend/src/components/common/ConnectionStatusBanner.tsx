import { useLayoutEffect, useRef, useSyncExternalStore } from 'react';
import { CloudOff, WifiOff } from 'lucide-react';
import { checkApiConnection, getConnectionSnapshot, subscribeToConnection } from '@/api/connectivity';
import { Button } from '@/components/ui/button';

export function ConnectionStatusBanner() {
  const { status, checking } = useSyncExternalStore(subscribeToConnection, getConnectionSnapshot);
  const banner = useRef<HTMLDivElement>(null);
  const offline = status === 'offline';

  useLayoutEffect(() => {
    const element = banner.current;
    if (!element) return;

    const pageStyle = document.documentElement.style;
    const updateHeight = () => pageStyle.setProperty('--app-connection-height', `${element.offsetHeight}px`);
    updateHeight();
    const observer = new ResizeObserver(updateHeight);
    observer.observe(element);
    return () => {
      observer.disconnect();
      pageStyle.removeProperty('--app-connection-height');
    };
  }, []);

  return (
    <div ref={banner} className="sticky top-0 z-40" data-connection-status={status}>
      <div className={status !== 'connected'
        ? 'flex flex-wrap items-center justify-center gap-x-3 gap-y-1 border-b border-warning/25 bg-warning-soft px-3 py-1.5 text-sm text-foreground'
        : undefined}>
        <p role="status" aria-atomic="true" className="flex items-center gap-2 font-medium">
          {offline && <><WifiOff aria-hidden="true" className="size-4 shrink-0 text-warning" /> Bạn đang ngoại tuyến</>}
          {status === 'unavailable' && <><CloudOff aria-hidden="true" className="size-4 shrink-0 text-warning" /> Không thể kết nối tới SQB</>}
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
