import { useInfiniteQuery } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';

interface UseReviewableSessionsParams {
  course_class_id?: number;
  sort_by?: string;
  limit?: number;
}

export const useReviewableSessions = ({
  course_class_id,
  sort_by,
  limit = 10,
}: UseReviewableSessionsParams = {}) => {
  return useInfiniteQuery({
    queryKey: ['reviewableSessions', { course_class_id, sort_by, limit }],
    queryFn: async ({ pageParam }) => {
      const res = await reviewService.getReviewableSessions({
        after: pageParam as number | undefined,
        limit,
        course_class_id,
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
