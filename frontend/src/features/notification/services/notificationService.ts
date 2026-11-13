import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  NotificationListResponseDto,
  ReadNotificationResponseDto,
  PushSettingsResponseDto,
  UpdatePushSettingsRequestDto,
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
      '/settings/push',
      { preserveEnumKeys: true }
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
      data,
      { preserveEnumKeys: true }
    );
  },
};
