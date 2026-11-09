import { useInfiniteQuery } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';

interface UseReviewableSessionsParams {
  course_class_id?: number;
  limit?: number;
}

export const useReviewableSessions = ({
  course_class_id,
  limit = 10,
}: UseReviewableSessionsParams = {}) => {
  return useInfiniteQuery({
    queryKey: ['reviewableSessions', { course_class_id, limit }],
    queryFn: async ({ pageParam }) => {
      const res = await reviewService.getReviewableSessions({
        after: pageParam,
        limit,
        course_class_id,
      });
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage, _pages, previous) => {
      const after = lastPage.pagination?.after;
      if (lastPage.pagination?.hasNext && after != null && after !== previous) {
        return after;
      }
      return undefined;
    },
    staleTime: 30 * 1000,
    refetchOnMount: 'always',
  });
};
