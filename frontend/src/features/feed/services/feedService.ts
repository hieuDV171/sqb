import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { CursorResponse } from '@/types/user.types';
import type { ActivityFeedItemDto, NewFeedCountResponseDto } from '../types/feed.types';

export const feedService = {
  /**
   * Lấy danh sách dòng hoạt động cá nhân hóa theo con trỏ Cursor
   * GET /api/v1/activity-feeds
   */
  getActivityFeeds: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> => {
    return (await axiosClient.get<GlobalResponse<CursorResponse<ActivityFeedItemDto>>>(
      '/activity-feeds',
      {
        params: { after, limit },
      }
    )) as any;
  },

  /**
   * Lấy số lượng tin hoạt động mới kể từ mốc thời gian chỉ định
   * GET /api/v1/activity-feeds/new-count
   */
  getNewFeedCount: async (since?: number): Promise<GlobalResponse<NewFeedCountResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<NewFeedCountResponseDto>>(
      '/activity-feeds/new-count',
      {
        params: { since },
      }
    )) as any;
  },
};
