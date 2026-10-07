import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse, CursorResponse } from '@/types/response.types';
import type {
  NotificationListResponseDto,
  ReadNotificationResponseDto,
  PushSettingsResponseDto,
  UpdatePushSettingsRequestDto,
  ActivityFeedItemDto,
  NewFeedCountResponseDto,
} from '../types/notification.types';

export const notificationService = {
  /**
   * 13.1 Lấy danh sách thông báo của tôi
   * GET /api/v1/notifications
   */
  getNotifications: async (params?: {
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<NotificationListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<NotificationListResponseDto>>(
      '/notifications',
      {
        params: {
          after: params?.after,
          limit: params?.limit ?? 20,
        },
      }
    )) as any;
  },

  /**
   * 13.2 Đánh dấu một thông báo đã đọc
   * POST /api/v1/notifications/{id}/read
   */
  markAsRead: async (
    notificationId: number
  ): Promise<GlobalResponse<ReadNotificationResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<ReadNotificationResponseDto>>(
      `/notifications/${notificationId}/read`
    )) as any;
  },

  /**
   * 13.3 Đánh dấu tất cả thông báo đã đọc
   * POST /api/v1/notifications/read-all
   */
  markAllAsRead: async (): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      '/notifications/read-all'
    )) as any;
  },

  /**
   * 13.4 Lấy cấu hình nhận thông báo đẩy
   * GET /api/v1/settings/push
   */
  getPushSettings: async (): Promise<GlobalResponse<PushSettingsResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<PushSettingsResponseDto>>(
      '/settings/push'
    )) as any;
  },

  /**
   * 13.5 Cập nhật cấu hình nhận thông báo đẩy
   * PUT /api/v1/settings/push
   */
  updatePushSettings: async (
    data: UpdatePushSettingsRequestDto
  ): Promise<GlobalResponse<PushSettingsResponseDto>> => {
    return (await axiosClient.put<GlobalResponse<PushSettingsResponseDto>>(
      '/settings/push',
      data
    )) as any;
  },

  /**
   * 14.1 Lấy dòng hoạt động cá nhân hóa
   * GET /api/v1/activity-feeds
   */
  getActivityFeeds: async (params?: {
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> => {
    return (await axiosClient.get<GlobalResponse<CursorResponse<ActivityFeedItemDto>>>(
      '/activity-feeds',
      {
        params: {
          after: params?.after,
          limit: params?.limit ?? 20,
        },
      }
    )) as any;
  },

  /**
   * 14.2 Đếm số lượng hoạt động mới
   * GET /api/v1/activity-feeds/new-count
   */
  getNewFeedCount: async (
    since?: number
  ): Promise<GlobalResponse<NewFeedCountResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<NewFeedCountResponseDto>>(
      '/activity-feeds/new-count',
      {
        params: { since },
      }
    )) as any;
  },
};
