import { useQuery, useInfiniteQuery, useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useStompStatus, useStompSubscription } from '@/lib/stompClient';
import { notificationService } from '../services/notificationService';
import type {
  UpdatePushSettingsRequestDto,
  NotificationListResponseDto,
} from '../types/notification.types';
import { toast } from '@/stores/useToastStore';
import { useGamificationStore } from '@/stores/useGamificationStore';

export const NOTIFICATION_KEYS = {
  all: ['notifications'] as const,
  lists: ['notifications', 'list'] as const,
  list: (limit: number) => [...NOTIFICATION_KEYS.lists, limit] as const,
  pushSettings: () => [...NOTIFICATION_KEYS.all, 'push-settings'] as const,
};

type NotificationPages = InfiniteData<NotificationListResponseDto>;

export function useNotificationClock() {
  const [now, setNow] = useState(Date.now);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(timer);
  }, []);
  return now;
}

export function useNotifications({ limit = 20 } = {}) {
  const setUnread = useGamificationStore((s) => s.setUnreadNotifications);

  return useInfiniteQuery({
    queryKey: NOTIFICATION_KEYS.list(limit),
    initialPageParam: undefined as number | undefined,
    queryFn: async ({ pageParam, signal }) => {
      const { data } = await notificationService.getNotifications({ after: pageParam, limit }, signal);
      if (!data) throw new Error('Không thể tải thông báo.');
      if (pageParam === undefined && !signal.aborted) setUnread(data.unreadCount);
      return data;
    },
    getNextPageParam: (page, _, __, pageParams) => {
      const cursor = page.pagination?.after;
      return page.pagination?.hasNext && typeof cursor === 'number' && !pageParams.includes(cursor) ? cursor : undefined;
    },
  });
}

export function useNotificationRealtime() {
  const queryClient = useQueryClient();
  const status = useStompStatus();

  useStompSubscription<{ eventType: string }>('/user/queue/notifications', (event) => {
    if (event?.eventType !== 'NOTIFICATIONS_CHANGED') return;

    const lists = { queryKey: NOTIFICATION_KEYS.lists };
    // Also replace an initial fetch that started before the new notification committed.
    void queryClient.cancelQueries(lists).then(() => queryClient.invalidateQueries(lists));
  });

  useEffect(() => {
    if (status !== 'CONNECTED') return;
    void queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.lists });
  }, [queryClient, status]);
}

export function useMarkAsRead() {
  const queryClient = useQueryClient();
  const setGamificationUnread = useGamificationStore((s) => s.setUnreadNotifications);

  return useMutation({
    mutationFn: (notificationId: number) =>
      notificationService.markAsRead(notificationId),
    onMutate: async (notificationId: number) => {
      // 1. Dừng các query notifications đang chạy để tránh xung đột
      await queryClient.cancelQueries({ queryKey: NOTIFICATION_KEYS.lists });

      // 2. Chụp snapshot cache hiện tại và số đếm unread
      const previousNotifications = queryClient.getQueriesData<NotificationPages>({
        queryKey: NOTIFICATION_KEYS.lists,
      });
      const previousUnreadCount = useGamificationStore.getState().unreadNotifications;

      // 3. Optimistic Update: Giảm badge đếm trên HUD Store ngay lập tức (không để âm)
      setGamificationUnread(Math.max(0, previousUnreadCount - 1));

      // Cập nhật ngay readAt cho thông báo đó trong cache (chấm xanh biến mất ở 0ms)
      const nowIso = new Date().toISOString();
      queryClient.setQueriesData<NotificationPages>(
        { queryKey: NOTIFICATION_KEYS.lists },
        (oldData) => oldData && ({
          ...oldData,
          pages: oldData.pages.map((page) => ({
            ...page,
            unreadCount: Math.max(0, page.unreadCount - 1),
            items: page.items.map((item) => item.notificationId === notificationId
              ? { ...item, readAt: item.readAt || nowIso }
              : item),
          })),
        })
      );

      return { previousNotifications, previousUnreadCount };
    },
    onError: (_err, _id, context) => {
      // 4. Rollback lại dữ liệu cũ nếu mất mạng/server lỗi
      if (context?.previousNotifications) {
        for (const [queryKey, data] of context.previousNotifications) {
          queryClient.setQueryData(queryKey, data);
        }
      }
      if (context?.previousUnreadCount !== undefined) {
        setGamificationUnread(context.previousUnreadCount);
      }
      toast.error('Không thể đánh dấu đã đọc thông báo.');
    },
    onSuccess: (res) => {
      // 5. Đồng bộ chính xác số lượng unread do server trả về
      if (res.data?.unreadCount !== undefined) {
        setGamificationUnread(res.data.unreadCount);
      }
    },
    onSettled: () => {
      // 6. Luôn làm mới ngầm sau cùng
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.lists });
    },
  });
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient();
  const setGamificationUnread = useGamificationStore((s) => s.setUnreadNotifications);

  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onMutate: async () => {
      // 1. Dừng các query notifications đang chạy
      await queryClient.cancelQueries({ queryKey: NOTIFICATION_KEYS.lists });

      // 2. Chụp snapshot cache hiện tại
      const previousNotifications = queryClient.getQueriesData<NotificationPages>({
        queryKey: NOTIFICATION_KEYS.lists,
      });
      const previousUnreadCount = useGamificationStore.getState().unreadNotifications;

      // 3. Optimistic Update: Xóa sạch unread về 0 ngay lập tức (0ms)
      setGamificationUnread(0);

      const nowIso = new Date().toISOString();
      queryClient.setQueriesData<NotificationPages>(
        { queryKey: NOTIFICATION_KEYS.lists },
        (oldData) => oldData && ({
          ...oldData,
          pages: oldData.pages.map((page) => ({
            ...page,
            unreadCount: 0,
            items: page.items.map((item) => ({ ...item, readAt: item.readAt || nowIso })),
          })),
        })
      );

      return { previousNotifications, previousUnreadCount };
    },
    onError: (_err, _vars, context) => {
      // 4. Rollback nếu có lỗi
      if (context?.previousNotifications) {
        for (const [queryKey, data] of context.previousNotifications) {
          queryClient.setQueryData(queryKey, data);
        }
      }
      if (context?.previousUnreadCount !== undefined) {
        setGamificationUnread(context.previousUnreadCount);
      }
      toast.error('Không thể đánh dấu đọc tất cả.');
    },
    onSuccess: () => {
      toast.success('Đã đánh dấu đọc tất cả thông báo');
    },
    onSettled: () => {
      // 5. Invalidate ngầm
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.lists });
    },
  });
}

export function usePushSettings(enabled = true) {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.pushSettings(),
    enabled,
    queryFn: async () => {
      const res = await notificationService.getPushSettings();
      return res.data;
    },
  });
}

export function useUpdatePushSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdatePushSettingsRequestDto) =>
      notificationService.updatePushSettings(data),
    onSuccess: () => {
      toast.success('Cập nhật cài đặt thông báo thành công');
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.pushSettings() });
    },
    onError: () => {
      toast.error('Lỗi khi cập nhật cài đặt thông báo');
    },
  });
}
