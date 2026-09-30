import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useReviewDetail } from '@/features/review/hooks/useReviewDetail';
import { useReviewMutations } from '@/features/review/hooks/useReviewMutations';
import { ReviewQuestionCard } from '@/features/review/components/ReviewQuestionCard';
import { RejectQuestionModal } from '@/features/review/components/RejectQuestionModal';
import { EditQuestionModal } from '@/features/review/components/EditQuestionModal';
import { CompleteReviewDialog } from '@/features/review/components/CompleteReviewDialog';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import type {
  SessionQuestionReviewDto,
  EditQuestionRequest,
} from '@/features/review/types/review.types';
import {
  ArrowLeft,
  Award,
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  XCircle,
  CheckCheck,
} from 'lucide-react';

export const ReviewWorkspacePage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const numericSessionId = Number(sessionId);

  const { data, isLoading, isError, refetch } = useReviewDetail(numericSessionId);
  const {
    approveQuestions,
    isApproving,
    rejectQuestions,
    isRejecting,
    editQuestion,
    isEditing,
    completeReview,
    isCompleting,
  } = useReviewMutations(numericSessionId);

  // Modal States
  const [rejectingQuestion, setRejectingQuestion] =
    useState<SessionQuestionReviewDto | null>(null);
  const [editingQuestion, setEditingQuestion] =
    useState<SessionQuestionReviewDto | null>(null);
  const [isCompleteDialogOpen, setIsCompleteDialogOpen] = useState(false);

  const questions = data?.items || [];
  const warnings = data?.duplicateWarnings || [];

  // Review statistics
  const totalCount = questions.length;
  const approvedCount = questions.filter((q) => q.status === 'APPROVED').length;
  const rejectedCount = questions.filter((q) => q.status === 'REJECTED').length;
  const pendingCount = questions.filter(
    (q) => q.status !== 'APPROVED' && q.status !== 'REJECTED'
  ).length;

  const progressPercent = totalCount > 0 ? (approvedCount / totalCount) * 100 : 0;

  // Actions
  const handleApproveSingle = async (questionId: number) => {
    await approveQuestions({ questionIds: [questionId] });
  };

  const handleApproveAllPending = async () => {
    const pendingIds = questions
      .filter((q) => q.status !== 'APPROVED' && q.status !== 'REJECTED')
      .map((q) => q.questionId);
    if (pendingIds.length === 0) return;
    await approveQuestions({ questionIds: pendingIds });
  };

  const handleConfirmReject = async (reason: string) => {
    if (!rejectingQuestion) return;
    await rejectQuestions({
      questionIds: [rejectingQuestion.questionId],
      reason,
    });
    setRejectingQuestion(null);
  };

  const handleSaveEdit = async (editData: EditQuestionRequest) => {
    if (!editingQuestion) return;
    await editQuestion({
      questionId: editingQuestion.questionId,
      data: editData,
    });
    setEditingQuestion(null);
  };

  const handleConfirmComplete = async () => {
    await completeReview(numericSessionId);
    setIsCompleteDialogOpen(false);
    navigate('/lecturer/sessions');
  };

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 pb-12">
        <div className="h-10 w-48 bg-slate-200 dark:bg-slate-800 rounded-2xl animate-pulse" />
        <div className="h-28 w-full bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 animate-pulse space-y-3">
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-4 w-1/2" />
        </div>
        <div className="space-y-4">
          <Skeleton className="h-64 w-full rounded-3xl" />
          <Skeleton className="h-64 w-full rounded-3xl" />
        </div>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="max-w-4xl mx-auto py-12">
        <ErrorState
          title="Không thể tải không gian duyệt đề xuất"
          message="Không tìm thấy phiên đề xuất hoặc bạn không có quyền truy cập."
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-16">
      {/* Top Navigation Bar */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <button
          type="button"
          onClick={() => navigate('/lecturer/sessions')}
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-2xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại hàng đợi duyệt</span>
        </button>

        <div className="flex items-center gap-2 flex-wrap">
          {pendingCount > 1 && (
            <button
              type="button"
              onClick={handleApproveAllPending}
              disabled={isApproving}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-2xl text-xs font-semibold bg-emerald-50 hover:bg-emerald-100 dark:bg-emerald-950/60 dark:hover:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800 transition-colors cursor-pointer shadow-2xs"
            >
              <CheckCheck className="w-3.5 h-3.5" />
              <span>Duyệt tất cả còn lại ({pendingCount})</span>
            </button>
          )}

          <button
            type="button"
            onClick={() => setIsCompleteDialogOpen(true)}
            disabled={isCompleting}
            className="inline-flex items-center gap-2 px-5 py-2 rounded-2xl text-xs md:text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-md shadow-indigo-500/25 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
          >
            <Award className="w-4 h-4" />
            <span>Hoàn tất duyệt phiên</span>
          </button>
        </div>
      </div>

      {/* Sticky Progress & Overview Header */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/90 dark:border-slate-800 p-5 md:p-6 shadow-xs space-y-4">
        <div className="flex items-start justify-between flex-wrap gap-2">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono font-bold px-2 py-0.5 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                Phiên #{numericSessionId}
              </span>
              <span className="text-xs font-semibold text-indigo-600 dark:text-indigo-400">
                Workspace Đánh giá Chuyên môn
              </span>
            </div>
            <h2 className="text-lg md:text-xl font-bold text-slate-900 dark:text-slate-100 mt-1">
              Đánh giá {totalCount} câu hỏi đề xuất
            </h2>
          </div>

          {/* Quick Counter Pills */}
          <div className="flex items-center gap-2 text-xs font-bold">
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
              <CheckCircle2 className="w-3.5 h-3.5" />
              {approvedCount} đã duyệt
            </span>

            {rejectedCount > 0 && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-rose-50 dark:bg-rose-950/50 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800">
                <XCircle className="w-3.5 h-3.5" />
                {rejectedCount} từ chối
              </span>
            )}

            {pendingCount > 0 && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800">
                <HelpCircle className="w-3.5 h-3.5" />
                {pendingCount} chờ xem
              </span>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px] font-semibold text-slate-500 dark:text-slate-400">
            <span>Tiến độ thẩm định</span>
            <span>
              {approvedCount + rejectedCount}/{totalCount} câu đã xử lý ({progressPercent.toFixed(0)}%)
            </span>
          </div>
          <div className="w-full h-2.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden flex">
            <div
              style={{ width: `${(approvedCount / totalCount) * 100}%` }}
              className="bg-emerald-500 transition-all duration-300"
            />
            <div
              style={{ width: `${(rejectedCount / totalCount) * 100}%` }}
              className="bg-rose-500 transition-all duration-300"
            />
          </div>
        </div>
      </div>

      {/* Duplicate Detection Warning Banner */}
      {warnings.length > 0 && (
        <div className="p-4 rounded-3xl bg-rose-50/80 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800/80 flex items-start gap-3 text-xs md:text-sm text-rose-900 dark:text-rose-200">
          <AlertTriangle className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <div className="font-bold">
              Phát hiện {warnings.length} cảnh báo trùng lặp trong phiên nộp này
            </div>
            <p className="text-xs text-rose-800/80 dark:text-rose-300/80 leading-relaxed">
              Hệ thống AI đối soát 3 tầng (Trigram từ ngữ, Vector ngữ nghĩa, và Mã băm ảnh P-Hash) đã phát hiện một số câu hỏi có độ tương đồng cao với ngân hàng đề thi hiện có. Vui lòng bấm vào huy hiệu cảnh báo trên từng câu hỏi để xem chi tiết đối sánh.
            </p>
          </div>
        </div>
      )}

      {/* Questions Review List */}
      <div className="space-y-5">
        {questions.map((question, idx) => {
          // Find warnings matching this question (by questionIndex or similar)
          const questionWarnings = warnings.filter(
            (w) => w.questionIndex === idx
          );

          return (
            <ReviewQuestionCard
              key={question.questionId}
              question={question}
              questionNumber={idx + 1}
              duplicateWarnings={questionWarnings}
              onApprove={handleApproveSingle}
              onReject={(q) => setRejectingQuestion(q)}
              onEdit={(q) => setEditingQuestion(q)}
              disabled={isApproving || isRejecting || isEditing}
            />
          );
        })}
      </div>

      {/* Bottom Sticky Action Floating Pill */}
      <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md rounded-2xl border border-slate-200 dark:border-slate-800 shadow-xl px-5 py-2.5 flex items-center gap-3">
        <span className="text-xs font-medium text-slate-700 dark:text-slate-300">
          Đã duyệt <strong>{approvedCount}</strong>/{totalCount} câu
        </span>
        <button
          type="button"
          onClick={() => setIsCompleteDialogOpen(true)}
          disabled={isCompleting}
          className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-md shadow-indigo-500/20 active:scale-95 transition-all cursor-pointer"
        >
          <Award className="w-3.5 h-3.5" />
          <span>Chốt phiên & Thưởng điểm</span>
        </button>
      </div>

      {/* Modals */}
      <RejectQuestionModal
        isOpen={!!rejectingQuestion}
        questionId={rejectingQuestion?.questionId || 0}
        questionNumber={
          rejectingQuestion
            ? questions.findIndex((q) => q.questionId === rejectingQuestion.questionId) + 1
            : 0
        }
        isRejecting={isRejecting}
        onClose={() => setRejectingQuestion(null)}
        onConfirm={handleConfirmReject}
      />

      <EditQuestionModal
        isOpen={!!editingQuestion}
        question={editingQuestion}
        questionNumber={
          editingQuestion
            ? questions.findIndex((q) => q.questionId === editingQuestion.questionId) + 1
            : 0
        }
        isSaving={isEditing}
        onClose={() => setEditingQuestion(null)}
        onSave={handleSaveEdit}
      />

      <CompleteReviewDialog
        isOpen={isCompleteDialogOpen}
        sessionId={numericSessionId}
        approvedCount={approvedCount}
        pendingCount={pendingCount}
        rejectedCount={rejectedCount}
        isCompleting={isCompleting}
        onClose={() => setIsCompleteDialogOpen(false)}
        onConfirm={handleConfirmComplete}
      />
    </div>
  );
};
