import { useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reactService, type ReactionType } from '@/features/feed/services/reactService';
import { updatePracticeQuestion } from '@/features/practice/hooks/updatePracticeQuestion';
import {
  ArrowLeft,
  Award,
} from 'lucide-react';
import {
  usePracticeSession,
  usePracticeAnswer,
  PracticeSkeleton,
  PracticeEmptyState,
  PracticeProgressBar,
  PracticeQuestionCard,
  PracticeSummaryModal,
  QuestionStatsModal,
  QuestionRateModal,
  QuestionDiscussionModal,
} from '@/features/practice';
import { ErrorState } from '@/components/ui/error-state';
import { Button } from '@/components/ui/button';
import { StudyHeader } from './StudyHeader';

export function PracticeWorkspacePage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const numericSessionId = Number(sessionId);
  return <PracticeWorkspace key={sessionId} numericSessionId={numericSessionId} />;
}

function PracticeWorkspace({ numericSessionId }: { numericSessionId: number }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: sessionData, isLoading, isError, error, refetch } =
    usePracticeSession(numericSessionId);
  const { mutateAsync: submitAnswerMutation, isPending: isSubmittingAnswer, error: answerError, variables: answerVariables } =
    usePracticeAnswer();

  // Local state for practice session
  const [requestedIndex, setCurrentIndex] = useState(0);
  const [selectedOptionsMap, setSelectedOptionsMap] = useState<Record<number, string[]>>({});
  const [userAnswersRecord, setUserAnswersRecord] = useState<
    Record<number, { isCorrect: boolean; selected: string[] }>
  >({});
  const reactionMutation = useMutation({
    mutationFn: ({ questionId, reactionType }: { questionId: number; reactionType: ReactionType }) =>
      reactService.toggleReact({ targetType: 'QUESTION', targetId: questionId, reactionType }),
    onSuccess: async ({ data }, { questionId }) => {
      if (!data) return;
      await queryClient.cancelQueries({ queryKey: ['practiceSession', numericSessionId] });
      updatePracticeQuestion(queryClient, numericSessionId, questionId, (question) => ({
        ...question,
        reactCount: data.totalCount,
        myInteraction: { ...question.myInteraction, myReaction: data.myReaction ?? null },
      }));
    },
  });

  // Modals state
  const [statsTarget, setStatsTarget] = useState<{
    id: number | null;
    code?: string;
  }>({ id: null });
  const [rateTarget, setRateTarget] = useState<{
    id: number | null;
    code?: string;
  }>({ id: null });
  const [discussionTarget, setDiscussionTarget] = useState<{
    id: number | null;
    code?: string;
  }>({ id: null });
  const [isSummaryOpen, setIsSummaryOpen] = useState<boolean>(false);
  const questionHeading = useRef<HTMLHeadingElement>(null);

  const questions = sessionData?.questions || [];
  const currentIndex = Math.min(requestedIndex, Math.max(0, questions.length - 1));
  const currentQuestion = questions[currentIndex];
  const ratedQuestion = questions.find((question) => question.questionId === rateTarget.id);
  const returnLink = (
    <Button variant="ghost" render={<Link to="/questions" />} nativeButton={false} className="min-h-11">
      <ArrowLeft aria-hidden="true" /> Ngân hàng câu hỏi
    </Button>
  );

  const selectQuestion = (index: number) => {
    setCurrentIndex(index);
    requestAnimationFrame(() => questionHeading.current?.focus());
  };

  // Handle option toggle for current question
  const handleToggleOption = (key: string) => {
    if (!currentQuestion) return;
    const qId = currentQuestion.questionId;
    const currentSelected = selectedOptionsMap[qId] || [];

    let newSelected: string[];
    if (currentSelected.includes(key)) {
      newSelected = currentSelected.filter((k) => k !== key);
    } else {
      newSelected = [...currentSelected, key];
    }

    setSelectedOptionsMap((prev) => ({
      ...prev,
      [qId]: newSelected,
    }));
  };

  // Submit Answer handler
  const handleSubmitAnswer = async () => {
    if (!currentQuestion) return;
    const qId = currentQuestion.questionId;
    const selected = selectedOptionsMap[qId] || [];
    if (selected.length === 0 || isSubmittingAnswer) return;

    try {
      const res = await submitAnswerMutation({
        questionId: qId,
        sessionId: numericSessionId,
        request: {
          selectedOptions: selected,
          questionUpdatedAt: currentQuestion.updatedAt,
        },
      });

      // Record answer result
      setUserAnswersRecord((prev) => ({
        ...prev,
        [qId]: {
          isCorrect: res.isCorrect,
          selected,
        },
      }));
    } catch {
      // The active question shows the mutation error.
    }
  };

  const handleReviewFromStart = () => {
    selectQuestion(0);
    setIsSummaryOpen(false);
  };

  if (isLoading) {
    return <PracticeSkeleton />;
  }

  if (isError || !sessionData) {
    const validId = Number.isSafeInteger(numericSessionId) && numericSessionId > 0;
    return (
      <div className="mx-auto max-w-4xl space-y-4">
        {returnLink}
        <ErrorState
          title="Không thể tải phiên luyện tập"
          message={
            (error as Error)?.message ||
            'Phiên câu hỏi không tồn tại hoặc bạn chưa được cấp quyền truy cập.'
          }
          onRetry={validId ? () => void refetch() : undefined}
        />
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <PracticeEmptyState
          title="Phiên này chưa có câu hỏi nào"
          message="Các câu hỏi trong phiên đang chờ giảng viên hoàn tất duyệt hoặc chưa được thêm vào."
          onAction={() => navigate('/questions')}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl space-y-5">
      {returnLink}
      <StudyHeader
        section="Luyện tập"
        title={sessionData.title}
        description={[sessionData.subjectName || sessionData.subjectCode, sessionData.sessionCode, `${questions.length} câu hỏi`].filter(Boolean).join(' · ')}
        action={(
          <Button variant="outline" onClick={() => setIsSummaryOpen(true)} className="min-h-11">
            <Award aria-hidden="true" /> Kết quả luyện tập
          </Button>
        )}
      />

      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_17rem]">
        <section className="min-w-0" aria-labelledby="active-practice-question">
          <h2
            ref={questionHeading}
            id="active-practice-question"
            tabIndex={-1}
            className="mb-3 scroll-mt-24 text-sm font-semibold text-academic focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
          >
            Câu {currentIndex + 1} / {questions.length}
          </h2>
          {currentQuestion && (
            <PracticeQuestionCard
              key={currentQuestion.questionId}
              question={currentQuestion}
              selectedOptions={selectedOptionsMap[currentQuestion.questionId] ?? []}
              onToggleOption={handleToggleOption}
              onSubmitAnswer={handleSubmitAnswer}
              isSubmittingAnswer={isSubmittingAnswer}
              answerError={answerVariables?.questionId === currentQuestion.questionId ? answerError?.message : undefined}
              onPrevQuestion={() => selectQuestion(currentIndex - 1)}
              onNextQuestion={() => selectQuestion(currentIndex + 1)}
              hasPrev={currentIndex > 0}
              hasNext={currentIndex < questions.length - 1}
              onOpenStats={(id, code) => setStatsTarget({ id, code })}
              onOpenRate={(id, code) => setRateTarget({ id, code })}
              onOpenDiscussion={(id, code) => setDiscussionTarget({ id, code })}
              localAnswerResult={userAnswersRecord[currentQuestion.questionId]}
              isUpdatingReaction={reactionMutation.isPending}
              onToggleReaction={() => reactionMutation.mutate({
                questionId: currentQuestion.questionId,
                reactionType: currentQuestion.myInteraction.myReaction ?? 'LIKE',
              })}
            />
          )}
        </section>
        <PracticeProgressBar
          questions={questions}
          currentIndex={currentIndex}
          onSelectQuestion={selectQuestion}
          userAnswersRecord={userAnswersRecord}
        />
      </div>

      {/* Stats Modal */}
      <QuestionStatsModal
        questionId={statsTarget.id}
        questionCode={statsTarget.code}
        isOpen={!!statsTarget.id}
        onClose={() => setStatsTarget({ id: null })}
      />

      {/* Rate Modal */}
      {ratedQuestion && (
        <QuestionRateModal
          key={ratedQuestion.questionId}
          questionId={ratedQuestion.questionId}
          questionCode={ratedQuestion.questionCode}
          sessionId={numericSessionId}
          interaction={ratedQuestion.myInteraction}
          onClose={() => setRateTarget({ id: null })}
        />
      )}

      {/* Discussion Modal */}
      <QuestionDiscussionModal
        sessionId={numericSessionId}
        questionId={discussionTarget.id}
        questionCode={discussionTarget.code}
        isOpen={!!discussionTarget.id}
        onClose={() => setDiscussionTarget({ id: null })}
      />

      {/* Summary Modal */}
      <PracticeSummaryModal
        isOpen={isSummaryOpen}
        onClose={() => setIsSummaryOpen(false)}
        questions={questions}
        userAnswersRecord={userAnswersRecord}
        onReviewFromStart={handleReviewFromStart}
        sessionTitle={sessionData.title}
        subjectName={sessionData.subjectName}
      />
    </div>
  );
}
