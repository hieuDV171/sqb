import { useInfiniteQuery } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';

interface UsePendingSessionsParams {
  subject_id?: number;
  sort_by?: string;
  limit?: number;
}

export const usePendingSessions = ({
  subject_id,
  sort_by,
  limit = 10,
}: UsePendingSessionsParams = {}) => {
  return useInfiniteQuery({
    queryKey: ['pendingSessions', { subject_id, sort_by, limit }],
    queryFn: async ({ pageParam }) => {
      const res = await reviewService.getPendingSessions({
        after: pageParam as number | undefined,
        limit,
        subject_id,
        sort_by,
      });
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage?.pagination?.hasNext && lastPage?.pagination?.after) {
        return lastPage.pagination.after;
      }
      return undefined;
    },
    staleTime: 30 * 1000,
  });
};
