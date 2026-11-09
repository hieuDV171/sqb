import { useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCheck, ClipboardCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useReviewDetail } from '@/features/review/hooks/useReviewDetail';
import { useReviewMutations } from '@/features/review/hooks/useReviewMutations';
import { ReviewQuestionCard } from '@/features/review/components/ReviewQuestionCard';
import { EditQuestionModal } from '@/features/review/components/EditQuestionModal';
import { CompleteReviewDialog } from '@/features/review/components/CompleteReviewDialog';
import type { DuplicateWarning, SessionQuestionReviewDto } from '@/features/review/types/review.types';

const queuePath = '/lecturer/sessions';

export function ReviewWorkspacePage() {
  const sessionId = Number(useParams().sessionId);
  const { state } = useLocation();
  const search = typeof state?.queueSearch === 'string' ? state.queueSearch : '';
  const queue = { pathname: queuePath, search: search ? `?${search}` : '' };

  if (!Number.isSafeInteger(sessionId) || sessionId < 1) {
    return (
      <div className="mx-auto max-w-5xl space-y-4">
        <Button variant="ghost" nativeButton={false} render={<Link to={queue} />}>
          <ArrowLeft aria-hidden="true" /> Phiên cần duyệt
        </Button>
        <ErrorState title="Đường dẫn phiên không hợp lệ" message="Mở lại phiên từ danh sách cần duyệt." />
      </div>
    );
  }
  return <ReviewWorkspace key={sessionId} sessionId={sessionId} queue={queue} />;
}

function ReviewWorkspace({ sessionId, queue }: { sessionId: number; queue: { pathname: string; search: string } }) {
  const navigate = useNavigate();
  const review = useReviewDetail(sessionId);
  const { approve, reject, edit, complete } = useReviewMutations(sessionId);
  const [editing, setEditing] = useState<SessionQuestionReviewDto | null>(null);
  const [confirmation, setConfirmation] = useState<'approve' | 'complete' | null>(null);

  const questions = review.data?.items ?? [];
  const warnings = review.data?.duplicateWarnings ?? [];
  const warningsByIndex = new Map<number, DuplicateWarning[]>();
  for (const warning of warnings) {
    const group = warningsByIndex.get(warning.questionIndex);
    if (group) group.push(warning);
    else warningsByIndex.set(warning.questionIndex, [warning]);
  }

  const total = questions.length;
  const approved = questions.filter((question) => question.status === 'APPROVED').length;
  const rejected = questions.filter((question) => question.status === 'REJECTED').length;
  const pendingIds = questions.filter((question) => question.status === 'PENDING').map((question) => question.questionId);
  const processed = approved + rejected;
  const finalized = review.data?.sessionStatus === 'RESOLVED';
  const canReview = review.data?.sessionStatus === 'REVIEWING' || review.data?.sessionStatus === 'PENDING';
  const busy = approve.isPending || reject.isPending || edit.isPending || complete.isPending;
  const actionsDisabled = !canReview || busy || review.isFetching || review.isRefetchError;

  async function confirm() {
    if (actionsDisabled) return;
    try {
      if (confirmation === 'approve' && pendingIds.length > 0) {
        await approve.mutateAsync({ questionIds: pendingIds });
        setConfirmation(null);
      } else if (confirmation === 'complete') {
        await complete.mutateAsync();
        navigate(queue, { replace: true });
      }
    } catch {
      // The mutation reports the error. Keep the confirmation open for retry.
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-5 pb-10">
      <Button variant="ghost" className="min-h-11" nativeButton={false} render={<Link to={queue} />}>
        <ArrowLeft aria-hidden="true" /> Phiên cần duyệt
      </Button>

      {review.isPending ? (
        <div role="status" aria-label="Đang tải phiên duyệt" className="space-y-4">
          <Skeleton className="h-28 rounded-2xl" />
          <Skeleton className="h-72 rounded-2xl" />
        </div>
      ) : !review.data ? (
        <ErrorState title="Không thể tải phiên duyệt"
          message={review.error?.message || 'Kiểm tra lại quyền truy cập hoặc thử tải lại.'}
          onRetry={() => void review.refetch()} />
      ) : (
        <>
          <header className="flex flex-col gap-4 border-b border-border pb-5 sm:flex-row sm:items-end sm:justify-between">
            <div className="min-w-0 space-y-2">
              <p className="flex items-center gap-2 text-sm font-semibold text-academic">
                <ClipboardCheck aria-hidden="true" className="size-4" /> Không gian giảng viên
              </p>
              <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Duyệt phiên #{sessionId}</h1>
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="outline" className={finalized ? 'bg-success-soft text-success' : 'bg-academic-soft text-academic'}>
                  {finalized ? 'Đã chốt · Chỉ đọc' : canReview ? 'Chưa chốt' : 'Chưa rõ trạng thái phiên'}
                </Badge>
                <p className="text-sm text-muted-foreground">{total} câu hỏi đề xuất</p>
              </div>
            </div>
            {canReview && total > 0 && (
              <div className="flex flex-wrap gap-2">
                {pendingIds.length > 1 && (
                  <Button variant="outline" className="min-h-11" disabled={actionsDisabled} onClick={() => setConfirmation('approve')}>
                    <CheckCheck aria-hidden="true" /> Duyệt các câu còn chờ
                  </Button>
                )}
                <Button className="min-h-11" disabled={actionsDisabled} onClick={() => setConfirmation('complete')}>Chốt phiên</Button>
              </div>
            )}
          </header>

          {review.isRefetchError && (
            <div role="alert" className="flex flex-wrap items-center gap-2 text-sm text-destructive">
              Không tải được trạng thái mới nhất.
              <Button variant="ghost" className="min-h-11" onClick={() => void review.refetch()}>Thử lại</Button>
            </div>
          )}

          {total === 0 ? (
            <EmptyState title="Phiên chưa có câu hỏi" description="Quay lại danh sách để chọn một phiên khác." />
          ) : (
            <div className="grid items-start gap-5 lg:grid-cols-[16rem_minmax(0,1fr)]">
              <aside aria-label="Tiến độ duyệt" className="space-y-4 rounded-2xl border border-border bg-card p-4">
                <div className="space-y-1.5">
                  <h2 className="font-semibold">Tiến độ duyệt</h2>
                  <p className="text-sm text-muted-foreground" aria-live="polite">{processed}/{total} câu đã xử lý</p>
                  <progress aria-label="Số câu đã xử lý" value={processed} max={total} className="h-2 w-full accent-academic" />
                </div>
                {processed === total && (
                  <p className="text-sm text-success">
                    {canReview ? 'Mọi câu đã có quyết định. Bạn có thể rà lại trước khi chốt.' : 'Mọi câu đã có quyết định.'}
                  </p>
                )}
                <dl className="space-y-1 text-sm">
                  <div className="flex justify-between gap-2"><dt>Chờ duyệt</dt><dd className="font-semibold tabular-nums">{pendingIds.length}</dd></div>
                  <div className="flex justify-between gap-2"><dt>Đã phê duyệt</dt><dd className="font-semibold tabular-nums text-success">{approved}</dd></div>
                  <div className="flex justify-between gap-2"><dt>Đã từ chối</dt><dd className="font-semibold tabular-nums text-destructive">{rejected}</dd></div>
                </dl>
                <nav aria-label="Đi đến câu hỏi" className="flex flex-wrap gap-1.5">
                  {questions.map((question, index) => {
                    const status = question.status === 'APPROVED' ? 'đã phê duyệt'
                      : question.status === 'REJECTED' ? 'đã từ chối' : 'chờ duyệt';
                    return (
                      <Button key={question.questionId} variant="outline" size="icon"
                        aria-label={`Câu ${index + 1}, ${status}`}
                        className={question.status === 'APPROVED' ? 'size-11 text-success'
                          : question.status === 'REJECTED' ? 'size-11 text-destructive' : 'size-11'}
                        nativeButton={false} render={<a href={`#review-question-${question.questionId}`} />}>
                        {index + 1}
                      </Button>
                    );
                  })}
                </nav>
              </aside>
              <section aria-label="Câu hỏi đề xuất" className="min-w-0 space-y-4">
                {warnings.length > 0 && (
                  <div className="flex gap-2 rounded-xl border border-warning/25 bg-warning-soft p-4 text-sm">
                    <AlertTriangle aria-hidden="true" className="size-5 shrink-0 text-warning" />
                    <p>Có <strong>{warnings.length} đối sánh tương đồng</strong>. Mở cảnh báo trên từng câu để đối chiếu trước khi duyệt.</p>
                  </div>
                )}
                {questions.map((question, index) => (
                  <ReviewQuestionCard key={question.questionId} question={question} questionNumber={index + 1}
                    duplicateWarnings={warningsByIndex.get(index) ?? []}
                    onApprove={(id) => approve.mutate({ questionIds: [id] })}
                    onReject={async (id, rejectionCode, reason) => {
                      await reject.mutateAsync({ questionIds: [id], rejectionCode, reason });
                    }}
                    onEdit={setEditing}
                    readOnly={!canReview}
                    disabled={actionsDisabled}
                  />
                ))}
              </section>
            </div>
          )}

          {canReview && editing && (
            <EditQuestionModal key={editing.questionId} question={editing}
              questionNumber={questions.findIndex((question) => question.questionId === editing.questionId) + 1}
              isSaving={edit.isPending} onClose={() => setEditing(null)}
              onSave={async (data) => {
                await edit.mutateAsync({ questionId: editing.questionId, data });
                setEditing(null);
              }} />
          )}
          {canReview && confirmation && (
            <CompleteReviewDialog mode={confirmation} pendingCount={pendingIds.length}
              isRefreshing={review.isFetching || review.isRefetchError}
              isSubmitting={approve.isPending || complete.isPending} onClose={() => setConfirmation(null)}
              onConfirm={() => void confirm()} />
          )}
        </>
      )}
    </div>
  );
}
