import { useMutation, useQueryClient } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import type {
  RateQuestionRequest,
  RateQuestionResponse,
  SessionQuestionsResponse,
} from '../types/practice.types';

interface RateMutationParams {
  questionId: number;
  sessionId?: number;
  request: RateQuestionRequest;
}

export function useQuestionRatingMutation() {
  const queryClient = useQueryClient();

  return useMutation<RateQuestionResponse, Error, RateMutationParams>({
    mutationFn: async ({ questionId, request }) => {
      const res = await practiceService.rateQuestion(questionId, request);
      if (!res.data) throw new Error(res.message || 'Không thể gửi đánh giá câu hỏi');
      return res.data;
    },
    onSuccess: (data, variables) => {
      const { questionId, sessionId } = variables;

      // Update practice session cache
      if (sessionId) {
        queryClient.setQueryData<SessionQuestionsResponse>(
          ['practiceSession', sessionId],
          (old) => {
            if (!old) return old;
            return {
              ...old,
              questions: old.questions.map((q) => {
                if (q.questionId === questionId) {
                  return {
                    ...q,
                    ratingCount: data.newRatingCount,
                    myInteraction: {
                      ...q.myInteraction,
                      rated: true,
                    },
                  };
                }
                return q;
              }),
            };
          }
        );
      }

      // Invalidate ratings and statistics queries
      queryClient.invalidateQueries({
        queryKey: ['questionRatings', questionId],
      });
      queryClient.invalidateQueries({
        queryKey: ['questionStatistics', questionId],
      });
    },
  });
}
