import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';

export type InteractionTargetType = 'POST' | 'COMMENT' | 'QUESTION' | 'SESSION' | 'BADGE_AWARD';
export type ReactionType = 'LIKE' | 'LOVE' | 'WOW';

export interface ReactRequest {
  targetType: InteractionTargetType;
  targetId: number;
  reactionType: ReactionType;
}

export interface ReactResponseDto {
  myReaction: ReactionType | null;
  reactionCounts: Record<string, number>;
  totalCount: number;
}

export const reactService = {
  /**
   * Thả hoặc hủy cảm xúc (Toggle React) cho bài viết hoặc bình luận
   * POST /api/v1/react
   */
  toggleReact: async (data: ReactRequest, options?: { skipToast?: boolean }): Promise<GlobalResponse<ReactResponseDto>> => {
    return axiosClient.post<GlobalResponse<ReactResponseDto>, GlobalResponse<ReactResponseDto>>('/react', data, options);
  },
};
