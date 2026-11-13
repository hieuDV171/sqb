import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { CursorResponse } from '@/types/user.types';
import type { ActivityFeedItemDto, NewFeedCountResponseDto } from '../types/feed.types';
import { normalizePostMedia, type ApiMedia } from '../utils/postMedia';

type ApiFeedItem = Omit<ActivityFeedItemDto, 'content'> & {
  content: Omit<ActivityFeedItemDto['content'], 'mediaUrls'> & { mediaUrls?: ApiMedia[] };
};

export const feedService = {
  /**
   * Lấy danh sách dòng hoạt động cá nhân hóa theo con trỏ Cursor
   * GET /api/v1/activity-feeds
   */
  getActivityFeeds: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> => {
    const response = await axiosClient.get<unknown, GlobalResponse<CursorResponse<ApiFeedItem>>>(
      '/activity-feeds', { params: { after, limit } },
    );
    return {
      ...response,
      data: {
        ...response.data,
        items: response.data.items.map((item) => ({
          ...item,
          content: { ...item.content, mediaUrls: normalizePostMedia(item.content.mediaUrls) },
        })),
      },
    };
  },

  /**
   * Lấy số lượng tin hoạt động mới kể từ mốc thời gian chỉ định
   * GET /api/v1/activity-feeds/new-count
   */
  getNewFeedCount: async (since?: number): Promise<GlobalResponse<NewFeedCountResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<NewFeedCountResponseDto>>(
      '/activity-feeds/new-count', { params: { since } },
    );
  },
};
