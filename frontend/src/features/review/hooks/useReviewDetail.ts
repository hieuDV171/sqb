import { useQuery } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';

export const useReviewDetail = (sessionId?: number) => {
  return useQuery({
    queryKey: ['reviewSessionDetail', sessionId],
    queryFn: async () => {
      if (!sessionId) return null;
      const res = await reviewService.getSessionDetailForReview(sessionId);
      return res.data;
    },
    enabled: !!sessionId,
    staleTime: 10 * 1000,
  });
};
