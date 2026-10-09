import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { searchService } from '../services/searchService';
import type { SearchType } from '../types/search.types';
import { toast } from '@/stores/useToastStore';
import type { SavedAndTrendingSearchesResponseDto } from '@/types/search.types';

export const SEARCH_KEYS = {
  all: ['search'] as const,
  query: (q?: string, type?: SearchType, after?: number, limit?: number) =>
    [...SEARCH_KEYS.all, 'query', q, type, after, limit] as const,
  savedAndTrending: () => [...SEARCH_KEYS.all, 'saved-trending'] as const,
};

export function useGlobalSearch(params: {
  query?: string;
  type?: SearchType;
  after?: number;
  limit?: number;
  enabled?: boolean;
}) {
  return useQuery({
    queryKey: SEARCH_KEYS.query(
      params.query,
      params.type,
      params.after,
      params.limit
    ),
    queryFn: async () => {
      const res = await searchService.search(params);
      return res.data;
    },
    enabled: params.enabled !== undefined ? params.enabled : !!params.query?.trim(),
  });
}

export function useSavedAndTrendingSearches(enabled = true) {
  return useQuery({
    queryKey: SEARCH_KEYS.savedAndTrending(),
    queryFn: async () => {
      const res = await searchService.getSavedAndTrendingSearches();
      return res.data;
    },
    enabled,
    staleTime: 60000,
  });
}

export function useDeleteSavedSearch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => searchService.deleteSavedSearch(id),
    onMutate: async (deletedId: number) => {
      // 1. Dừng các query đang chạy để tránh ghi đè
      await queryClient.cancelQueries({ queryKey: SEARCH_KEYS.savedAndTrending() });

      // 2. Lưu snapshot cache cũ để phục vụ rollback nếu server lỗi
      const previousData = queryClient.getQueryData<SavedAndTrendingSearchesResponseDto>(
        SEARCH_KEYS.savedAndTrending()
      );

      // 3. Optimistic Update: Xóa ngay lập tức khỏi UI (Zero Perceived Latency)
      if (previousData) {
        queryClient.setQueryData<SavedAndTrendingSearchesResponseDto>(
          SEARCH_KEYS.savedAndTrending(),
          {
            ...previousData,
            savedSearches: previousData.savedSearches.filter(
              (item) => item.savedSearchId !== deletedId
            ),
          }
        );
      }

      return { previousData };
    },
    onError: (_err, _deletedId, context) => {
      // 4. Rollback lại danh sách cũ từ context nếu có lỗi mạng/server
      if (context?.previousData) {
        queryClient.setQueryData(SEARCH_KEYS.savedAndTrending(), context.previousData);
      }
      toast.error('Không thể xóa từ khóa tìm kiếm. Vui lòng kiểm tra kết nối mạng!');
    },
    onSettled: () => {
      // 5. Luôn làm mới lại dữ liệu từ server sau khi hoàn tất
      queryClient.invalidateQueries({
        queryKey: SEARCH_KEYS.savedAndTrending(),
      });
    },
  });
}
