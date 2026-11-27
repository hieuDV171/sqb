import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { feedService } from '../services/feedService';

export function useNewFeedCount(enabled = true) {
  const [lastRefreshedAt, setLastRefreshedAt] = useState<number>(() => Date.now());
  const { data } = useQuery({
    queryKey: ['activity-feeds-new-count', lastRefreshedAt],
    enabled,
    queryFn: async () => {
      const res = await feedService.getNewFeedCount(lastRefreshedAt);
      return res.data?.newCount || 0;
    },
    refetchInterval: false,
    refetchOnWindowFocus: 'always',
    staleTime: 0,
  });

  return {
    newCount: data || 0,
    markRefreshed: setLastRefreshedAt,
  };
}
