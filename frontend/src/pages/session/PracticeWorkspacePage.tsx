import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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

export const PracticeWorkspacePage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const numericSessionId = Number(sessionId);

  const { data: sessionData, isLoading, isError, error, refetch } =
    usePracticeSession(numericSessionId);
  const { mutateAsync: submitAnswerMutation, isPending: isSubmittingAnswer } =
    usePracticeAnswer();

  // Local state for practice session
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [selectedOptionsMap, setSelectedOptionsMap] = useState<Record<number, string[]>>({});
  const [userAnswersRecord, setUserAnswersRecord] = useState<
    Record<number, { isCorrect: boolean; selected: string[] }>
  >({});

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

  const questions = sessionData?.questions || [];
  const currentQuestion = questions[currentIndex];

  // Initialize selected options from existing answered questions if any
  useEffect(() => {
    if (questions.length > 0) {
      const initialAnswers: Record<number, { isCorrect: boolean; selected: string[] }> = {};
      questions.forEach((q) => {
        if (q.myInteraction.answered && q.hiddenFields?.correctAnswer) {
          initialAnswers[q.questionId] = {
            isCorrect: true, // Default indicator if answered previously
            selected: q.hiddenFields.correctAnswer.split(',').map((s) => s.trim()),
          };
        }
      });
      if (Object.keys(initialAnswers).length > 0) {
        setUserAnswersRecord((prev) => ({ ...initialAnswers, ...prev }));
      }
    }
  }, [questions]);

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
    if (selected.length === 0) return;

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
    } catch (err) {
      console.error('Failed to submit answer:', err);
    }
  };

  // Restart practice session
  const handleRestartPractice = () => {
    setUserAnswersRecord({});
    setSelectedOptionsMap({});
    setCurrentIndex(0);
    setIsSummaryOpen(false);
  };

  if (isLoading) {
    return <PracticeSkeleton />;
  }

  if (isError || !sessionData) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <ErrorState
          title="Không thể tải phiên luyện tập"
          message={
            (error as Error)?.message ||
            'Phiên câu hỏi không tồn tại hoặc bạn chưa được cấp quyền truy cập.'
          }
          onRetry={() => refetch()}
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
    <div className="max-w-4xl mx-auto px-3 sm:px-6 py-6 sm:py-8 space-y-6">
      {/* Top Breadcrumb & Session Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200/80 dark:border-slate-800 pb-5">
        <div>
          <button
            type="button"
            onClick={() => navigate('/questions')}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 mb-2 transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại Ngân hàng câu hỏi</span>
          </button>

          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-lg text-xs font-black bg-indigo-50 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60">
              {sessionData.subjectCode || 'MÔN HỌC'}
            </span>
            <h1 className="text-lg sm:text-xl font-black text-slate-900 dark:text-white tracking-tight line-clamp-1">
              {sessionData.title}
            </h1>
          </div>
        </div>

        {/* Action button: Bảng điểm tổng kết */}
        <button
          type="button"
          onClick={() => setIsSummaryOpen(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-600 dark:text-amber-400 text-xs sm:text-sm font-bold transition-all cursor-pointer self-start sm:self-auto border border-amber-500/20 shadow-2xs"
        >
          <Award className="w-4 h-4" />
          <span>Tổng kết kết quả</span>
        </button>
      </div>

      {/* Progress Bar & Question Palette */}
      <PracticeProgressBar
        questions={questions}
        currentIndex={currentIndex}
        onSelectQuestion={(idx) => setCurrentIndex(idx)}
        userAnswersRecord={userAnswersRecord}
        onOpenSummary={() => setIsSummaryOpen(true)}
      />

      {/* Active Question Card */}
      {currentQuestion && (
        <PracticeQuestionCard
          question={currentQuestion}
          questionIndex={currentIndex}
          totalQuestions={questions.length}
          selectedOptions={selectedOptionsMap[currentQuestion.questionId] || []}
          onToggleOption={handleToggleOption}
          onSubmitAnswer={handleSubmitAnswer}
          isSubmittingAnswer={isSubmittingAnswer}
          onPrevQuestion={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
          onNextQuestion={() =>
            setCurrentIndex((prev) => Math.min(questions.length - 1, prev + 1))
          }
          hasPrev={currentIndex > 0}
          hasNext={currentIndex < questions.length - 1}
          onOpenStats={(id, code) => setStatsTarget({ id, code })}
          onOpenRate={(id, code) => setRateTarget({ id, code })}
          onOpenDiscussion={(id, code) => setDiscussionTarget({ id, code })}
          localAnswerResult={userAnswersRecord[currentQuestion.questionId]}
        />
      )}

      {/* Stats Modal */}
      <QuestionStatsModal
        questionId={statsTarget.id}
        questionCode={statsTarget.code}
        isOpen={!!statsTarget.id}
        onClose={() => setStatsTarget({ id: null })}
      />

      {/* Rate Modal */}
      <QuestionRateModal
        questionId={rateTarget.id}
        questionCode={rateTarget.code}
        sessionId={numericSessionId}
        isOpen={!!rateTarget.id}
        onClose={() => setRateTarget({ id: null })}
      />

      {/* Discussion Modal */}
      <QuestionDiscussionModal
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
        onRestartPractice={handleRestartPractice}
        sessionTitle={sessionData.title}
        subjectName={sessionData.subjectName}
      />
    </div>
  );
};
