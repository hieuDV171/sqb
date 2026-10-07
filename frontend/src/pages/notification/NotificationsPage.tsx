import { useState, useMemo } from 'react';
import {
  CheckCheck,
  Settings,
  RefreshCw,
  SlidersHorizontal,
  Inbox,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import {
  useNotifications,
  useMarkAllAsRead,
  NotificationItem,
  PushSettingsModal,
  type NotificationType,
} from '@/features/notification';

export function NotificationsPage() {
  const [filterType, setFilterType] = useState<'ALL' | 'UNREAD' | 'ACADEMIC' | 'SOCIAL' | 'GAMIFICATION'>('ALL');
  const [isPushSettingsOpen, setIsPushSettingsOpen] = useState(false);
  const [cursor, setCursor] = useState<number | undefined>(undefined);

  const {
    data: notificationData,
    isLoading,
    isFetching,
    refetch,
  } = useNotifications({
    after: cursor,
    limit: 20,
  });

  const markAllMutation = useMarkAllAsRead();

  const allItems = notificationData?.items || [];
  const unreadCount = notificationData?.unreadCount ?? 0;

  // Filter items
  const filteredNotifications = useMemo(() => {
    if (filterType === 'ALL') return allItems;
    if (filterType === 'UNREAD') return allItems.filter((n) => !n.readAt);

    const academicTypes: NotificationType[] = [
      'QUESTION_APPROVED',
      'QUESTION_ERROR_PENALTY',
    ];
    const socialTypes: NotificationType[] = ['FRIEND_REQUEST', 'NEW_COMMENT'];
    const gamificationTypes: NotificationType[] = [
      'EARNED_BADGE',
      'GAME5_SEMESTER_RECAP',
      'LEADERBOARD_HONOR',
    ];

    if (filterType === 'ACADEMIC') {
      return allItems.filter((n) => academicTypes.includes(n.type));
    }
    if (filterType === 'SOCIAL') {
      return allItems.filter((n) => socialTypes.includes(n.type));
    }
    if (filterType === 'GAMIFICATION') {
      return allItems.filter((n) => gamificationTypes.includes(n.type));
    }

    return allItems;
  }, [allItems, filterType]);

  const tabs: { key: typeof filterType; label: string }[] = [
    { key: 'ALL', label: 'Tất cả' },
    { key: 'UNREAD', label: `Chưa đọc ${unreadCount > 0 ? `(${unreadCount})` : ''}` },
    { key: 'ACADEMIC', label: 'Học thuật' },
    { key: 'SOCIAL', label: 'Bạn bè & Tương tác' },
    { key: 'GAMIFICATION', label: 'Huy hiệu & BXH' },
  ];

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
              Trung Tâm Thông Báo
            </h1>
            <Badge variant="role">Hệ thống</Badge>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Cập nhật hoạt động học thuật, lời mời kết bạn, kết quả thẩm định và thành tích vinh danh.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {unreadCount > 0 && (
            <button
              type="button"
              onClick={() => markAllMutation.mutate()}
              disabled={markAllMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-xl bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-950/40 dark:hover:bg-indigo-900/50 text-indigo-700 dark:text-indigo-300 transition-colors cursor-pointer"
            >
              <CheckCheck className="w-4 h-4" />
              Đánh dấu đã đọc tất cả
            </button>
          )}

          <button
            type="button"
            onClick={() => setIsPushSettingsOpen(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors shadow-xs cursor-pointer"
          >
            <Settings className="w-4 h-4" />
            Cài đặt
          </button>
        </div>
      </div>

      {/* Filter Tabs Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white dark:bg-slate-900 p-2 sm:p-3 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-xs">
        <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0">
          <SlidersHorizontal className="w-4 h-4 text-slate-400 ml-2 hidden sm:block" />
          {tabs.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setFilterType(tab.key)}
              className={`px-3.5 py-1.5 text-xs font-bold rounded-xl transition-all whitespace-nowrap cursor-pointer ${
                filterType === tab.key
                  ? 'bg-indigo-600 text-white shadow-xs shadow-indigo-500/20'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={() => refetch()}
          disabled={isFetching}
          className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer ml-auto"
          title="Làm mới"
        >
          <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* List Container */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-start gap-4 animate-pulse">
                <div className="w-10 h-10 rounded-2xl bg-slate-200 dark:bg-slate-800 shrink-0" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-1/3" />
                  <div className="h-3 bg-slate-100 dark:bg-slate-800/60 rounded w-2/3" />
                </div>
              </div>
            ))}
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div className="py-16 px-4 text-center">
            <div className="w-16 h-16 rounded-3xl bg-indigo-50 dark:bg-indigo-950/40 flex items-center justify-center text-indigo-500 mx-auto mb-4">
              <Inbox className="w-8 h-8" />
            </div>
            <h3 className="text-base font-bold text-slate-800 dark:text-slate-200">
              Không có thông báo nào trong mục này
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Khi có tương tác mới, hệ thống sẽ tự động gửi thông báo theo các cài đặt nhận tin của bạn.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800/80 p-2">
            {filteredNotifications.map((item) => (
              <NotificationItem key={item.notificationId} notification={item} />
            ))}
          </div>
        )}

        {/* Cursor Pagination Load More */}
        {notificationData?.pagination?.hasNext && (
          <div className="p-4 border-t border-slate-100 dark:border-slate-800 text-center bg-slate-50/50 dark:bg-slate-900/50">
            <button
              type="button"
              onClick={() => {
                const last = allItems[allItems.length - 1];
                if (last) setCursor(last.notificationId);
              }}
              disabled={isFetching}
              className="px-5 py-2 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-white dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl transition-colors cursor-pointer"
            >
              {isFetching ? 'Đang tải thêm...' : 'Xem thông báo cũ hơn'}
            </button>
          </div>
        )}
      </div>

      {/* Push Settings Modal */}
      <PushSettingsModal
        isOpen={isPushSettingsOpen}
        onClose={() => setIsPushSettingsOpen(false)}
      />
    </div>
  );
}
