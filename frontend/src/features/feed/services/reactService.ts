import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';

export type InteractionTargetType = 'POST' | 'COMMENT' | 'QUESTION' | 'SESSION';
export type ReactionType = 'LIKE' | 'LOVE' | 'WOW';

export interface ReactRequest {
  targetType: InteractionTargetType;
  targetId: number;
  reactionType: ReactionType;
}

export interface ReactResponseDto {
  myReaction: string | null;
  reactionCounts: Record<string, number>;
  totalCount: number;
}

export const reactService = {
  /**
   * Thả hoặc hủy cảm xúc (Toggle React) cho bài viết hoặc bình luận
   * POST /api/v1/react
   */
  toggleReact: async (data: ReactRequest): Promise<GlobalResponse<ReactResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<ReactResponseDto>>('/react', data)) as any;
  },
};
