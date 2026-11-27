import { useQuery } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import type { QuestionStatisticsResponse } from '../types/practice.types';

export function useQuestionStatistics(questionId: number | null | undefined, enabled = true) {
  return useQuery<QuestionStatisticsResponse>({
    queryKey: ['questionStatistics', questionId],
    queryFn: async () => {
      if (!questionId) throw new Error('Question ID is required');
      const res = await practiceService.getQuestionStatistics(questionId);
      if (!res.data) throw new Error(res.message || 'Không thể tải thống kê câu hỏi');
      return res.data;
    },
    enabled: !!questionId && enabled,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
}
