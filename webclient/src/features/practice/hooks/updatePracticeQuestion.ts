import type { QueryClient } from '@tanstack/react-query';
import type { PracticeQuestionDto, SessionQuestionsResponse } from '@/features/practice';

export function updatePracticeQuestion(
  queryClient: QueryClient,
  sessionId: number,
  questionId: number,
  update: (question: PracticeQuestionDto) => PracticeQuestionDto,
) {
  queryClient.setQueryData<SessionQuestionsResponse>(['practiceSession', sessionId], (session) =>
    session && {
      ...session,
      questions: session.questions.map((question) => question.questionId === questionId ? update(question) : question),
    },
  );
}
