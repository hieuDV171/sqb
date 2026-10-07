import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  SearchType,
  GlobalSearchResponseDto,
  SavedAndTrendingSearchesResponseDto,
  DeleteSavedSearchResponseDto,
} from '../types/search.types';

export const searchService = {
  /**
   * 31.1 Tìm kiếm toàn cục đa thực thể
   * GET /api/v1/search
   */
  search: async (params: {
    query?: string;
    type?: SearchType;
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<GlobalSearchResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<GlobalSearchResponseDto>>(
      '/search',
      {
        params: {
          query: params.query,
          type: params.type || 'ALL',
          after: params.after,
          limit: params.limit ?? 10,
        },
      }
    )) as any;
  },

  /**
   * 31.2 Lấy lịch sử tìm kiếm cá nhân & Top từ khóa thịnh hành
   * GET /api/v1/search/saved
   */
  getSavedAndTrendingSearches: async (): Promise<
    GlobalResponse<SavedAndTrendingSearchesResponseDto>
  > => {
    return (await axiosClient.get<
      GlobalResponse<SavedAndTrendingSearchesResponseDto>
    >('/search/saved')) as any;
  },

  /**
   * 31.3 Xóa một từ khóa khỏi lịch sử tìm kiếm cá nhân
   * DELETE /api/v1/search/saved/{id}
   */
  deleteSavedSearch: async (
    id: number
  ): Promise<GlobalResponse<DeleteSavedSearchResponseDto>> => {
    return (await axiosClient.delete<
      GlobalResponse<DeleteSavedSearchResponseDto>
    >(`/search/saved/${id}`)) as any;
  },
};
