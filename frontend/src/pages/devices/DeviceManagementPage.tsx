import { useRef, useState } from 'react';
import { AlertDialog } from '@base-ui/react/alert-dialog';
import { AlertCircle, Clock, LogOut, Monitor, RefreshCw, ShieldCheck, Smartphone, Trash2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useDevices } from '@/hooks/useDevices';
import { cn } from '@/lib/utils';
import { toast } from '@/stores/useToastStore';
import type { DeviceResponse } from '@/types/device.types';

type DeviceAction = { type: 'revoke' | 'delete'; device: DeviceResponse };

const gridClass = 'grid gap-4 @min-[38rem]:grid-cols-2 @min-[61rem]:grid-cols-3';

function DeviceCard({ device, isCurrent, onAction }: {
  device: DeviceResponse;
  isCurrent: boolean;
  onAction: (type: DeviceAction['type'], trigger: HTMLButtonElement) => void;
}) {
  const isMobile = device.platform === 'IOS' || device.platform === 'ANDROID';
  const Icon = isMobile ? Smartphone : Monitor;
  const name = device.deviceName || (isMobile ? 'Thiết bị di động' : 'Trình duyệt web');

  return (
    <Card className={cn('min-w-0 rounded-2xl border-border', isCurrent && 'border-academic/40 ring-1 ring-academic/20')}>
      <CardContent className="flex flex-1 flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className={cn('grid size-11 shrink-0 place-items-center rounded-xl bg-muted text-muted-foreground',
            isCurrent && 'bg-academic-soft text-academic')}>
            <Icon aria-hidden="true" className="size-5" />
          </span>
          {isCurrent ? (
            <Badge variant="outline" className="border-academic/25 bg-academic-soft text-academic">
              <ShieldCheck aria-hidden="true" className="size-3.5" /> Thiết bị này
            </Badge>
          ) : device.isActive ? (
            <Badge variant="outline" className="border-success/25 bg-success-soft text-success">
              <span aria-hidden="true" className="size-1.5 rounded-full bg-current" /> Đang hoạt động
            </Badge>
          ) : (
            <Badge variant="outline" className="bg-muted text-muted-foreground">
              <Clock aria-hidden="true" className="size-3.5" /> Phiên đã lưu
            </Badge>
          )}
        </div>

        <div className="min-w-0">
          <h2 className="font-semibold wrap-anywhere">{name}</h2>
          <p className="mt-1 truncate font-mono text-xs text-muted-foreground" title={device.deviceId}>ID {device.deviceId}</p>
        </div>

        <dl className="grid gap-2 rounded-xl border border-border bg-surface-subtle p-3 text-sm">
          <div className="flex flex-wrap justify-between gap-x-2">
            <dt className="text-muted-foreground">Nền tảng</dt>
            <dd className="font-medium">{device.platform || '—'}</dd>
          </div>
          {device.osVersion && (
            <div className="flex flex-wrap justify-between gap-x-2">
              <dt className="text-muted-foreground">Hệ điều hành</dt>
              <dd className="font-medium wrap-anywhere">{device.osVersion}</dd>
            </div>
          )}
          {device.lastActiveAt && (
            <div className="flex flex-wrap justify-between gap-x-2">
              <dt className="text-muted-foreground">Hoạt động gần nhất</dt>
              <dd className="font-medium tabular-nums">
                <time dateTime={device.lastActiveAt}>{new Date(device.lastActiveAt).toLocaleString('vi-VN')}</time>
              </dd>
            </div>
          )}
        </dl>

        {isCurrent && <p className="text-xs leading-relaxed text-muted-foreground">Để thoát thiết bị này, dùng Đăng xuất trong menu tài khoản.</p>}

        <div className="mt-auto flex flex-wrap gap-2 border-t border-border pt-3">
          <Button type="button" variant="outline" disabled={isCurrent} className="min-h-11 flex-1"
            onClick={(event) => onAction('revoke', event.currentTarget)}>
            <LogOut aria-hidden="true" /> Đăng xuất
          </Button>
          <Button type="button" variant="destructive" disabled={isCurrent} className="min-h-11"
            aria-label={`Xóa thiết bị ${name}`} onClick={(event) => onAction('delete', event.currentTarget)}>
            <Trash2 aria-hidden="true" /> Xóa
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export function DeviceManagementPage() {
  const { devices, isLoading, error, fetchDevices, revokeDevice, deleteDevice, currentDeviceId } = useDevices();
  const [action, setAction] = useState<DeviceAction | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const actionTrigger = useRef<HTMLButtonElement>(null);
  const refreshButton = useRef<HTMLButtonElement>(null);

  async function confirmAction() {
    if (!action || isSubmitting || action.device.isCurrentDevice || action.device.deviceId === currentDeviceId) return;
    setIsSubmitting(true);
    setActionError(null);
    try {
      if (action.type === 'revoke') {
        await revokeDevice(action.device.deviceId);
        toast.success('Đã đăng xuất thiết bị');
      } else {
        await deleteDevice(action.device.deviceId);
        toast.success('Đã xóa thiết bị khỏi danh sách');
      }
      setAction(null);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Thao tác thất bại. Hãy thử lại.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="@container mx-auto max-w-6xl space-y-5 pb-8">
      <header className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-border bg-card p-4 sm:p-6">
        <div className="flex min-w-0 items-start gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
            <ShieldCheck aria-hidden="true" className="size-5" />
          </span>
          <div className="min-w-0">
            <h1 className="text-xl font-bold tracking-tight sm:text-2xl">Quản lý thiết bị</h1>
            <p className="mt-1 text-sm text-muted-foreground">Xem thiết bị đã đăng nhập và quản lý phiên từ xa.</p>
          </div>
        </div>
        <Button ref={refreshButton} type="button" variant="outline" className="min-h-11"
          onClick={() => void fetchDevices()} disabled={isLoading || isSubmitting}>
          <RefreshCw aria-hidden="true" className={cn(isLoading && 'motion-safe:animate-spin')} />
          {isLoading ? 'Đang tải…' : 'Làm mới'}
        </Button>
      </header>

      {isLoading && devices.length === 0 && (
        <div className={gridClass} role="status">
          <span className="sr-only">Đang tải danh sách thiết bị</span>
          {[1, 2, 3].map((index) => <Skeleton key={index} className="h-64 rounded-2xl" aria-hidden="true" />)}
        </div>
      )}

      {error && !isLoading && (devices.length === 0 ? (
        <ErrorState title="Không thể tải thiết bị" message={error} onRetry={() => void fetchDevices()} />
      ) : (
        <p role="alert" className="flex items-start gap-2 rounded-xl border border-destructive/25 bg-destructive/10 p-3.5 text-sm text-destructive">
          <AlertCircle aria-hidden="true" className="mt-0.5 size-4 shrink-0" /> {error}
        </p>
      ))}

      {!isLoading && !error && devices.length === 0 && (
        <EmptyState icon={Monitor} title="Chưa ghi nhận thiết bị nào" description="Thiết bị sẽ xuất hiện ở đây sau khi đăng nhập vào tài khoản của bạn." />
      )}

      {devices.length > 0 && (
        <section className={gridClass} aria-label="Danh sách thiết bị" aria-busy={isLoading}>
          {devices.map((device) => (
            <DeviceCard key={device.userDeviceId || device.deviceId} device={device}
              isCurrent={device.isCurrentDevice || device.deviceId === currentDeviceId}
              onAction={(type, trigger) => {
                actionTrigger.current = trigger;
                setActionError(null);
                setAction({ type, device });
              }} />
          ))}
        </section>
      )}

      <AlertDialog.Root open={action !== null} onOpenChange={(open) => {
        if (!open && !isSubmitting) setAction(null);
      }}>
        {action && (
          <AlertDialog.Portal>
            <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
            <AlertDialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl"
              finalFocus={() => actionTrigger.current?.isConnected ? actionTrigger.current : refreshButton.current}>
              <div className="flex items-start gap-3">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-destructive/10 text-destructive">
                  {action.type === 'delete' ? <Trash2 aria-hidden="true" className="size-5" /> : <LogOut aria-hidden="true" className="size-5" />}
                </span>
                <div className="min-w-0">
                  <AlertDialog.Title className="text-lg font-bold">
                    {action.type === 'delete' ? 'Xóa thiết bị khỏi danh sách?' : 'Đăng xuất thiết bị từ xa?'}
                  </AlertDialog.Title>
                  <p className="mt-1 text-sm wrap-anywhere text-muted-foreground">{action.device.deviceName || action.device.deviceId}</p>
                </div>
              </div>
              <AlertDialog.Description className="text-sm leading-relaxed text-muted-foreground">
                {action.type === 'delete'
                  ? 'Thiết bị sẽ bị gỡ khỏi danh sách và phiên đăng nhập trên thiết bị đó sẽ bị thu hồi.'
                  : 'Thiết bị này sẽ cần đăng nhập lại để tiếp tục sử dụng tài khoản.'}
              </AlertDialog.Description>
              {actionError && <p role="alert" className="text-sm text-destructive">{actionError}</p>}
              <div className="flex flex-wrap justify-end gap-2">
                <AlertDialog.Close render={<Button type="button" variant="outline" disabled={isSubmitting} />}>Hủy</AlertDialog.Close>
                <Button type="button" variant="destructive" disabled={isSubmitting} onClick={() => void confirmAction()}>
                  {isSubmitting ? 'Đang xử lý…' : action.type === 'delete' ? 'Xóa thiết bị' : 'Đăng xuất thiết bị'}
                </Button>
              </div>
            </AlertDialog.Popup>
          </AlertDialog.Portal>
        )}
      </AlertDialog.Root>
    </div>
  );
}
