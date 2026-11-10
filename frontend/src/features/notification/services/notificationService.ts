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
  }, signal?: AbortSignal): Promise<GlobalResponse<NotificationListResponseDto>> => {
    return axiosClient.get<GlobalResponse<NotificationListResponseDto>, GlobalResponse<NotificationListResponseDto>>(
      '/notifications',
      {
        signal,
        params: {
          after: params?.after,
          limit: params?.limit ?? 20,
        },
      }
    );
  },

  /**
   * 13.2 Đánh dấu một thông báo đã đọc
   * POST /api/v1/notifications/{id}/read
   */
  markAsRead: async (
    notificationId: number
  ): Promise<GlobalResponse<ReadNotificationResponseDto>> => {
    return axiosClient.post<GlobalResponse<ReadNotificationResponseDto>, GlobalResponse<ReadNotificationResponseDto>>(
      `/notifications/${notificationId}/read`
    );
  },

  /**
   * 13.3 Đánh dấu tất cả thông báo đã đọc
   * POST /api/v1/notifications/read-all
   */
  markAllAsRead: async (): Promise<GlobalResponse<void>> => {
    return axiosClient.post<GlobalResponse<void>, GlobalResponse<void>>(
      '/notifications/read-all'
    );
  },

  /**
   * 13.4 Lấy cấu hình nhận thông báo đẩy
   * GET /api/v1/settings/push
   */
  getPushSettings: async (): Promise<GlobalResponse<PushSettingsResponseDto>> => {
    return axiosClient.get<GlobalResponse<PushSettingsResponseDto>, GlobalResponse<PushSettingsResponseDto>>(
      '/settings/push'
    );
  },

  /**
   * 13.5 Cập nhật cấu hình nhận thông báo đẩy
   * PUT /api/v1/settings/push
   */
  updatePushSettings: async (
    data: UpdatePushSettingsRequestDto
  ): Promise<GlobalResponse<PushSettingsResponseDto>> => {
    return axiosClient.put<GlobalResponse<PushSettingsResponseDto>, GlobalResponse<PushSettingsResponseDto>>(
      '/settings/push',
      data
    );
  },

  /**
   * 14.1 Lấy dòng hoạt động cá nhân hóa
   * GET /api/v1/activity-feeds
   */
  getActivityFeeds: async (params?: {
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> => {
    return axiosClient.get<GlobalResponse<CursorResponse<ActivityFeedItemDto>>, GlobalResponse<CursorResponse<ActivityFeedItemDto>>>(
      '/activity-feeds',
      {
        params: {
          after: params?.after,
          limit: params?.limit ?? 20,
        },
      }
    );
  },

  /**
   * 14.2 Đếm số lượng hoạt động mới
   * GET /api/v1/activity-feeds/new-count
   */
  getNewFeedCount: async (
    since?: number
  ): Promise<GlobalResponse<NewFeedCountResponseDto>> => {
    return axiosClient.get<GlobalResponse<NewFeedCountResponseDto>, GlobalResponse<NewFeedCountResponseDto>>(
      '/activity-feeds/new-count',
      {
        params: { since },
      }
    );
  },
};
