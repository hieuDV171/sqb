import { useMutation, useQueryClient } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import type {
  AnswerQuestionRequest,
  AnswerQuestionResponse,
  SessionQuestionsResponse,
} from '../types/practice.types';

interface AnswerMutationParams {
  questionId: number;
  sessionId?: number;
  request: AnswerQuestionRequest;
}

export function usePracticeAnswer() {
  const queryClient = useQueryClient();

  return useMutation<AnswerQuestionResponse, Error, AnswerMutationParams>({
    mutationFn: async ({ questionId, request }) => {
      const res = await practiceService.answerQuestion(questionId, request);
      if (!res.data) throw new Error(res.message || 'Không thể nộp câu trả lời');
      return res.data;
    },
    onSuccess: (data, variables) => {
      const { questionId, sessionId } = variables;

      // Update practice session cache if sessionId is provided
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
                    myInteraction: {
                      ...q.myInteraction,
                      answered: true,
                    },
                    hiddenFields: {
                      correctAnswer: data.correctAnswer,
                      explanation: data.explanation,
                    },
                  };
                }
                return q;
              }),
            };
          }
        );
      }

      // Invalidate question statistics so the stats modal reflects new community answer
      queryClient.invalidateQueries({
        queryKey: ['questionStatistics', questionId],
      });
    },
  });
}
