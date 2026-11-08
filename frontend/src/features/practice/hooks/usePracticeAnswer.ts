import { useMutation, useQueryClient } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import { updatePracticeQuestion } from './updatePracticeQuestion';
import type {
  AnswerQuestionRequest,
  AnswerQuestionResponse,
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
    onSuccess: async (data, { questionId, sessionId, request }) => {

      if (sessionId) {
        await queryClient.cancelQueries({ queryKey: ['practiceSession', sessionId] });
        updatePracticeQuestion(queryClient, sessionId, questionId, (question) => ({
          ...question,
          myInteraction: { ...question.myInteraction, answered: true },
          hiddenFields: {
            correctAnswer: data.correctAnswer,
            explanation: data.explanation,
            selectedOptions: question.options
              .filter(({ key }) => request.selectedOptions.includes(key))
              .map(({ key, text }) => ({ key, text })),
            wasCorrect: data.isCorrect,
          },
        }));
      }

      // Invalidate question statistics so the stats modal reflects new community answer
      queryClient.invalidateQueries({
        queryKey: ['questionStatistics', questionId],
      });
    },
  });
}
