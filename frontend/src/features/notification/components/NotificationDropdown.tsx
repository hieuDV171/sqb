import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCheck, Settings, ArrowRight, Loader2, Inbox } from 'lucide-react';
import { useNotifications, useMarkAllAsRead, useNotificationClock } from '../hooks/useNotification';
import { NotificationItem } from './NotificationItem';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { PushSettingsModal } from './PushSettingsModal';

interface NotificationDropdownProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NotificationDropdown({ isOpen, onClose }: NotificationDropdownProps) {
  const [isPushSettingsOpen, setIsPushSettingsOpen] = useState(false);
  const now = useNotificationClock();

  const { data, isLoading, isLoadingError, refetch } = useNotifications({ limit: 6, poll: true });
  const notificationData = data?.pages[0];
  const markAllMutation = useMarkAllAsRead();

  const notifications = notificationData?.items || [];
  const unreadCount = notificationData?.unreadCount ?? 0;

  return (
    <>
      {isOpen && <div
        id="header-notifications"
        role="dialog"
        aria-labelledby="header-notifications-title"
        className="fixed inset-x-4 top-[var(--app-header-height)] z-50 mt-2 sm:absolute sm:inset-x-auto sm:right-0 sm:top-full sm:w-96 overflow-hidden rounded-2xl border border-border bg-popover text-popover-foreground shadow-xl"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border bg-surface-subtle">
          <div className="flex items-center gap-2">
            <h3 id="header-notifications-title" className="text-lg font-semibold text-foreground">
              Thông báo
            </h3>
            {unreadCount > 0 && (
              <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-academic-soft text-academic">
                {unreadCount} mới
              </span>
            )}
          </div>

          <div className="flex items-center gap-1">
            {unreadCount > 0 && (
              <Button
                variant="ghost" size="icon"
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending}
                className="size-11 rounded-xl text-muted-foreground hover:bg-muted hover:text-foreground"
                title="Đánh dấu tất cả đã đọc" aria-label="Đánh dấu tất cả đã đọc"
              >
                <CheckCheck aria-hidden="true" />
              </Button>
            )}

            <Button
              variant="ghost" size="icon"
              onClick={() => { setIsPushSettingsOpen(true); onClose(); }}
              className="size-11 text-muted-foreground"
              title="Cài đặt thông báo" aria-label="Cài đặt thông báo"
            >
              <Settings aria-hidden="true" />
            </Button>
          </div>
        </div>

        {/* Notification List */}
        <div className="max-h-[min(24rem,50dvh)] space-y-1 overflow-y-auto overscroll-contain p-1">
          {isLoading ? (
            <div role="status" className="flex items-center justify-center gap-2 py-10 text-sm text-muted-foreground">
              <Loader2 aria-hidden="true" className="size-5 motion-safe:animate-spin" />
              Đang tải thông báo...
            </div>
          ) : isLoadingError ? (
            <ErrorState title="Không thể tải thông báo" onRetry={() => void refetch()} />
          ) : notifications.length === 0 ? (
            <div className="py-12 px-4 text-center">
              <div className="mx-auto mb-3 grid size-12 place-items-center rounded-2xl bg-academic-soft text-academic">
                <Inbox className="w-6 h-6" />
              </div>
              <p className="text-sm font-semibold text-foreground">
                Bạn chưa có thông báo nào
              </p>
              <p className="text-sm text-muted-foreground mt-1 max-w-[220px] mx-auto">
                Khi có người kết bạn, duyệt câu hỏi hoặc tặng huy hiệu, thông báo sẽ hiển thị ở đây.
              </p>
            </div>
          ) : (
            notifications.map((item) => (
              <NotificationItem
                key={item.notificationId}
                notification={item}
                now={now}
                onItemClick={onClose}
              />
            ))
          )}
        </div>

        {/* Footer Link */}
        <div className="p-2 border-t border-border bg-surface-subtle text-center">
          <Button variant="ghost" nativeButton={false} render={<Link to="/notifications" />}
            onClick={onClose}
            className="min-h-11 w-full text-primary"
          >
            <span>Xem tất cả thông báo</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>}

      {/* Push Settings Modal */}
      <PushSettingsModal
        isOpen={isPushSettingsOpen}
        onClose={() => setIsPushSettingsOpen(false)}
      />
    </>
  );
}
