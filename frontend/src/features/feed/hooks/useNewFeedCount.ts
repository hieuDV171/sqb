import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { feedService } from '../services/feedService';

export function useNewFeedCount() {
  const [lastRefreshedAt, setLastRefreshedAt] = useState<number>(() => Date.now());
  const queryClient = useQueryClient();

  const { data, refetch } = useQuery({
    queryKey: ['activity-feeds-new-count', lastRefreshedAt],
    queryFn: async () => {
      const res = await feedService.getNewFeedCount(lastRefreshedAt);
      return res.data?.newCount || 0;
    },
    // Tắt hoàn toàn việc tự động poll định kỳ gây cấn/giật màn hình
    // Người dùng hoàn toàn chủ động refresh bằng thao tác kéo vuốt hoặc nút bấm
    refetchInterval: false,
    refetchOnWindowFocus: false,
    staleTime: 60000,
  });

  const resetCount = useCallback(() => {
    setLastRefreshedAt(Date.now());
  }, []);

  const triggerRefresh = useCallback(async () => {
    setLastRefreshedAt(Date.now());
    await queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
  }, [queryClient]);

  return {
    newCount: data || 0,
    lastRefreshedAt,
    resetCount,
    triggerRefresh,
    checkNewCount: refetch,
  };
}
