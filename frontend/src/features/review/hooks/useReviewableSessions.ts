import { useQuery } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';
import type { GetReviewableSessionsParams } from '../types/review.types';

export function useReviewableSessions(params: GetReviewableSessionsParams = {}) {
  return useQuery({
    queryKey: ['reviewableSessions', params],
    queryFn: async () => (await reviewService.getReviewableSessions(params)).data,
    staleTime: 30_000,
    refetchOnMount: 'always',
  });
}
