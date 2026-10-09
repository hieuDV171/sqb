import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import type {
  UpdatePushSettingsRequestDto,
  NotificationListResponseDto,
} from '../types/notification.types';
import { toast } from '@/stores/useToastStore';
import { useGamificationStore } from '@/stores/useGamificationStore';

export const NOTIFICATION_KEYS = {
  all: ['notifications'] as const,
  list: (after?: number, limit?: number) =>
    [...NOTIFICATION_KEYS.all, 'list', after, limit] as const,
  pushSettings: () => [...NOTIFICATION_KEYS.all, 'push-settings'] as const,
  activityFeeds: (after?: number) => ['activity-feeds', after] as const,
  newFeedCount: (since?: number) => ['activity-feeds', 'new-count', since] as const,
};

export function useNotifications(params?: { after?: number; limit?: number }) {
  const setGamificationUnread = useGamificationStore((s) => s.setUnreadNotifications);

  return useQuery({
    queryKey: NOTIFICATION_KEYS.list(params?.after, params?.limit),
    queryFn: async () => {
      const res = await notificationService.getNotifications(params);
      if (res.data?.unreadCount !== undefined) {
        setGamificationUnread(res.data.unreadCount);
      }
      return res.data;
    },
    refetchInterval: 30000, // Poll every 30s as fallback if websocket reconnects
  });
}

export function useMarkAsRead() {
  const queryClient = useQueryClient();
  const setGamificationUnread = useGamificationStore((s) => s.setUnreadNotifications);

  return useMutation({
    mutationFn: (notificationId: number) =>
      notificationService.markAsRead(notificationId),
    onMutate: async (notificationId: number) => {
      // 1. Dừng các query notifications đang chạy để tránh xung đột
      await queryClient.cancelQueries({ queryKey: NOTIFICATION_KEYS.all });

      // 2. Chụp snapshot cache hiện tại và số đếm unread
      const previousNotifications = queryClient.getQueriesData<NotificationListResponseDto>({
        queryKey: NOTIFICATION_KEYS.all,
      });
      const previousUnreadCount = useGamificationStore.getState().unreadNotifications;

      // 3. Optimistic Update: Giảm badge đếm trên HUD Store ngay lập tức (không để âm)
      setGamificationUnread(Math.max(0, previousUnreadCount - 1));

      // Cập nhật ngay readAt cho thông báo đó trong cache (chấm xanh biến mất ở 0ms)
      const nowIso = new Date().toISOString();
      queryClient.setQueriesData<NotificationListResponseDto>(
        { queryKey: NOTIFICATION_KEYS.all },
        (oldData) => {
          if (!oldData?.items) return oldData;
          return {
            ...oldData,
            unreadCount: Math.max(0, (oldData.unreadCount || 1) - 1),
            items: oldData.items.map((item) =>
              item.notificationId === notificationId
                ? { ...item, readAt: item.readAt || nowIso }
                : item
            ),
          };
        }
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
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.all });
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
      await queryClient.cancelQueries({ queryKey: NOTIFICATION_KEYS.all });

      // 2. Chụp snapshot cache hiện tại
      const previousNotifications = queryClient.getQueriesData<NotificationListResponseDto>({
        queryKey: NOTIFICATION_KEYS.all,
      });
      const previousUnreadCount = useGamificationStore.getState().unreadNotifications;

      // 3. Optimistic Update: Xóa sạch unread về 0 ngay lập tức (0ms)
      setGamificationUnread(0);

      const nowIso = new Date().toISOString();
      queryClient.setQueriesData<NotificationListResponseDto>(
        { queryKey: NOTIFICATION_KEYS.all },
        (oldData) => {
          if (!oldData?.items) return oldData;
          return {
            ...oldData,
            unreadCount: 0,
            items: oldData.items.map((item) => ({
              ...item,
              readAt: item.readAt || nowIso,
            })),
          };
        }
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
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.all });
    },
  });
}

export function usePushSettings() {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.pushSettings(),
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

export function useActivityFeeds(params?: { after?: number; limit?: number }) {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.activityFeeds(params?.after),
    queryFn: async () => {
      const res = await notificationService.getActivityFeeds(params);
      return res.data;
    },
  });
}

export function useNewFeedCount(since?: number) {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.newFeedCount(since),
    queryFn: async () => {
      const res = await notificationService.getNewFeedCount(since);
      return res.data;
    },
    enabled: !!since,
    refetchInterval: 15000,
  });
}
