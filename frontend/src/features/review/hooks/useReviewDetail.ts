import { useQuery } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';

export const useReviewDetail = (sessionId: number) => {
  return useQuery({
    queryKey: ['reviewSessionDetail', sessionId],
    queryFn: async () => {
      const res = await reviewService.getSessionDetailForReview(sessionId);
      return res.data;
    },
    staleTime: 10 * 1000,
    refetchOnMount: 'always',
  });
};
