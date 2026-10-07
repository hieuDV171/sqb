import { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { CheckCheck, Settings, ArrowRight, Loader2, Inbox } from 'lucide-react';
import { useNotifications, useMarkAllAsRead } from '../hooks/useNotification';
import { NotificationItem } from './NotificationItem';
import { PushSettingsModal } from './PushSettingsModal';

interface NotificationDropdownProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NotificationDropdown({ isOpen, onClose }: NotificationDropdownProps) {
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [isPushSettingsOpen, setIsPushSettingsOpen] = useState(false);

  const { data: notificationData, isLoading } = useNotifications({ limit: 6 });
  const markAllMutation = useMarkAllAsRead();

  const notifications = notificationData?.items || [];
  const unreadCount = notificationData?.unreadCount ?? 0;

  // Handle click outside to close
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node) &&
        !isPushSettingsOpen
      ) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen, onClose, isPushSettingsOpen]);

  if (!isOpen) return null;

  return (
    <>
      <div
        ref={dropdownRef}
        className="absolute right-0 top-full mt-2 w-80 sm:w-96 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
          <div className="flex items-center gap-2">
            <h3 className="font-bold text-slate-900 dark:text-slate-100 text-sm">
              Thông Báo
            </h3>
            {unreadCount > 0 && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700 dark:bg-indigo-900/50 dark:text-indigo-300">
                {unreadCount} mới
              </span>
            )}
          </div>

          <div className="flex items-center gap-1">
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending}
                className="p-1.5 rounded-xl text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-white dark:hover:bg-slate-800 transition-colors cursor-pointer"
                title="Đánh dấu tất cả đã đọc"
              >
                <CheckCheck className="w-4 h-4" />
              </button>
            )}

            <button
              type="button"
              onClick={() => setIsPushSettingsOpen(true)}
              className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-white dark:hover:bg-slate-800 transition-colors cursor-pointer"
              title="Cài đặt thông báo"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Notification List */}
        <div className="max-h-[380px] overflow-y-auto divide-y divide-slate-100/60 dark:divide-slate-800/60 p-1">
          {isLoading ? (
            <div className="flex items-center justify-center py-12 text-slate-400 text-xs">
              <Loader2 className="w-5 h-5 animate-spin mr-2" />
              Đang tải thông báo...
            </div>
          ) : notifications.length === 0 ? (
            <div className="py-12 px-4 text-center">
              <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 mx-auto mb-3">
                <Inbox className="w-6 h-6" />
              </div>
              <p className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Bạn chưa có thông báo nào
              </p>
              <p className="text-[11px] text-slate-400 mt-1 max-w-[220px] mx-auto">
                Khi có người kết bạn, duyệt câu hỏi hoặc tặng huy hiệu, thông báo sẽ hiển thị ở đây.
              </p>
            </div>
          ) : (
            notifications.map((item) => (
              <NotificationItem
                key={item.notificationId}
                notification={item}
                onItemClick={onClose}
              />
            ))
          )}
        </div>

        {/* Footer Link */}
        <div className="p-2 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 text-center">
          <Link
            to="/notifications"
            onClick={onClose}
            className="w-full py-2 px-3 rounded-xl text-xs font-bold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 dark:hover:text-indigo-300 hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30 flex items-center justify-center gap-1 transition-colors"
          >
            <span>Xem tất cả thông báo</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {/* Push Settings Modal */}
      <PushSettingsModal
        isOpen={isPushSettingsOpen}
        onClose={() => setIsPushSettingsOpen(false)}
      />
    </>
  );
}
