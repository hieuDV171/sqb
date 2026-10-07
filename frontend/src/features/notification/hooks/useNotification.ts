import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import type { UpdatePushSettingsRequestDto } from '../types/notification.types';
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
    onSuccess: (res) => {
      if (res.data?.unreadCount !== undefined) {
        setGamificationUnread(res.data.unreadCount);
      }
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.all });
    },
  });
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient();
  const setGamificationUnread = useGamificationStore((s) => s.setUnreadNotifications);

  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      setGamificationUnread(0);
      toast.success('Đã đánh dấu đọc tất cả thông báo');
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.all });
    },
    onError: () => {
      toast.error('Không thể đánh dấu đọc tất cả');
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
