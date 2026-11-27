import { useQuery } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import type { SessionQuestionsResponse } from '../types/practice.types';

export function usePracticeSession(sessionId: number | null | undefined) {
  return useQuery<SessionQuestionsResponse>({
    queryKey: ['practiceSession', sessionId],
    queryFn: async () => {
      if (!sessionId) throw new Error('Session ID is required');
      const res = await practiceService.getSessionQuestions(sessionId);
      if (!res.data) throw new Error(res.message || 'Không thể tải câu hỏi làm bài');
      return res.data;
    },
    enabled: !!sessionId && !isNaN(sessionId) && sessionId > 0,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}
