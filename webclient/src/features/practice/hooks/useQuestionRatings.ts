import { useInfiniteQuery } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import type { QuestionRatingsResponse } from '../types/practice.types';

export function useQuestionRatings(questionId: number | null | undefined, enabled = true) {
  return useInfiniteQuery<QuestionRatingsResponse>({
    queryKey: ['questionRatings', questionId],
    queryFn: async ({ pageParam }) => {
      if (!questionId) throw new Error('Question ID is required');
      const res = await practiceService.getQuestionRatings(
        questionId,
        pageParam as number | undefined,
        10
      );
      if (!res.data) throw new Error(res.message || 'Không thể tải đánh giá câu hỏi');
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage.pagination?.hasNext && lastPage.pagination.after) {
        return lastPage.pagination.after;
      }
      return undefined;
    },
    enabled: !!questionId && enabled,
    staleTime: 1000 * 60 * 3,
  });
}
