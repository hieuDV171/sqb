import { useState } from 'react';
import { CheckCheck, Settings, RefreshCw, Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { cn } from '@/lib/utils';
import { useNotifications, useMarkAllAsRead, useNotificationClock, NotificationItem, PushSettingsModal } from '@/features/notification';

const filters = [
  ['ALL', 'Tất cả'],
  ['UNREAD', 'Chưa đọc'],
  ['ACADEMIC', 'Học thuật'],
  ['SOCIAL', 'Bạn bè và tương tác'],
  ['GAMIFICATION', 'Thành tích'],
] as const;
type Filter = typeof filters[number][0];

const categories = {
  ACADEMIC: ['QUESTION_APPROVED', 'QUESTION_ERROR_PENALTY'],
  SOCIAL: ['FRIEND_REQUEST', 'NEW_COMMENT'],
  GAMIFICATION: ['EARNED_BADGE', 'GAME5_SEMESTER_RECAP', 'LEADERBOARD_HONOR'],
};

export function NotificationsPage() {
  const [filter, setFilter] = useState<Filter>('ALL');
  const [showSettings, setShowSettings] = useState(false);
  const now = useNotificationClock();
  const notifications = useNotifications();
  const markAll = useMarkAllAsRead();
  const unreadCount = notifications.data?.pages[0]?.unreadCount ?? 0;
  const items = (notifications.data?.pages.flatMap((page) => page.items) ?? []).filter((item) => {
    if (filter === 'ALL') return true;
    if (filter === 'UNREAD') return !item.readAt;
    return categories[filter].includes(item.type);
  });
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    notifications.hasNextPage && !notifications.isFetching && !notifications.isFetchNextPageError && items.length > 0,
    notifications.fetchNextPage,
    notifications.data?.pages.length ?? 0,
  );

  return (
    <div className="mx-auto max-w-4xl space-y-5 sm:space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-5">
        <div>
          <p className="mb-2 flex items-center gap-2 text-sm font-semibold text-academic"><Bell aria-hidden="true" className="size-4" /> Không gian của bạn</p>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Thông báo</h1>
          <p className="mt-2 text-sm text-muted-foreground">Theo dõi cập nhật học tập, tương tác và thành tích.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {unreadCount > 0 && (
            <Button variant="secondary" className="min-h-11" disabled={markAll.isPending} onClick={() => markAll.mutate()}>
              <CheckCheck aria-hidden="true" /> Đánh dấu tất cả đã đọc
            </Button>
          )}
          <Button variant="outline" className="min-h-11" onClick={() => setShowSettings(true)}>
            <Settings aria-hidden="true" /> Cài đặt
          </Button>
        </div>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-border bg-card p-2.5">
        <div role="group" aria-label="Lọc thông báo" className="flex flex-wrap gap-1">
          {filters.map(([key, label]) => (
            <Button key={key} variant="ghost" aria-pressed={filter === key} onClick={() => setFilter(key)}
              className={cn('min-h-11 px-3', filter === key ? 'bg-academic-soft text-academic hover:bg-academic-soft hover:text-academic' : 'text-muted-foreground')}>
              {label}{key === 'UNREAD' && unreadCount > 0 ? ' (' + unreadCount + ')' : ''}
            </Button>
          ))}
        </div>
        <Button variant="ghost" size="icon" className="ml-auto size-11 text-muted-foreground" aria-label="Làm mới thông báo"
          disabled={notifications.isFetching} onClick={() => void notifications.refetch()}>
          <RefreshCw aria-hidden="true" className={cn(notifications.isFetching && 'motion-safe:animate-spin')} />
        </Button>
      </div>

      {notifications.isLoading ? (
        <div aria-busy="true" aria-label="Đang tải thông báo" className="space-y-2">
          {[1, 2, 3, 4].map((row) => <Skeleton key={row} className="h-24 rounded-2xl" />)}
        </div>
      ) : notifications.isLoadingError ? (
        <ErrorState title="Không thể tải thông báo" onRetry={() => void notifications.refetch()} />
      ) : items.length ? (
        <ul aria-label="Danh sách thông báo" className="space-y-1 rounded-2xl border border-border bg-card p-2">
          {items.map((item) => <li key={item.notificationId}><NotificationItem notification={item} now={now} /></li>)}
        </ul>
      ) : (
        <EmptyState icon={Bell} title={notifications.hasNextPage ? 'Chưa có thông báo phù hợp trong các mục đã tải' : 'Không có thông báo trong mục này'}
          description={notifications.hasNextPage ? 'Bạn có thể xem thêm thông báo cũ hơn.' : 'Thông báo mới sẽ xuất hiện ở đây.'} />
      )}

      {notifications.hasNextPage && (
        <div className="space-y-2 text-center">
          <div ref={sentinel} aria-hidden="true" className="h-px" />
          {notifications.isFetchNextPageError && <p role="status" className="text-sm text-destructive">Không tải được thông báo cũ hơn.</p>}
          {notifications.isFetchingNextPage ? (
            <p role="status" className="text-sm text-muted-foreground">Đang tải thêm thông báo…</p>
          ) : (!supportsAutoLoad || notifications.isFetchNextPageError || !items.length) && (
            <Button variant="outline" className="min-h-11" disabled={notifications.isFetching} onClick={() => void notifications.fetchNextPage({ cancelRefetch: false })}>
              {notifications.isFetchNextPageError ? 'Thử tải lại' : 'Xem thông báo cũ hơn'}
            </Button>
          )}
        </div>
      )}
      <PushSettingsModal isOpen={showSettings} onClose={() => setShowSettings(false)} />
    </div>
  );
}
