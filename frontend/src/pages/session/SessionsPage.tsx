import { useState, useMemo } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import {
  useEnrolledSubjects,
  useMySubmissions,
  useSubmissionDetail,
  useSessionMutations,
  SubmissionFilterBar,
  SubmissionList,
  SubmissionDetailModal,
  EditSubmissionModal,
  DeleteSubmissionDialog,
  ProposeSessionForm,
  type SessionStatus,
  type SubmissionSessionSummaryDto,
  type MySubmissionDetailResponse,
  type UpdateSubmissionSessionRequest,
} from '@/features/session';
import { StudyHeader } from './StudyHeader';
import { StudyNavigation } from './StudyNavigation';

export function SessionsPage() {
  const [searchParams] = useSearchParams();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const isProposing = pathname.replace(/\/$/, '') === '/sessions/propose' || searchParams.get('tab') === 'propose';

  const setTab = (tab: 'submissions' | 'propose') => {
    navigate(tab === 'propose' ? '/sessions/propose' : '/questions');
  };

  // Filter States
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [selectedStatus, setSelectedStatus] = useState<SessionStatus | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState('');

  // Modals & Active Session States
  const [detailSessionId, setDetailSessionId] = useState<number | null>(null);
  const [editDetail, setEditDetail] = useState<MySubmissionDetailResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{
    sessionId: number;
    sessionCode: string;
  } | null>(null);

  // Queries & Mutations
  const subjectQuery = useEnrolledSubjects(!isProposing);

  const {
    data: submissionsPages,
    isLoading: isLoadingSubmissions,
    isLoadingError: isErrorSubmissions,
    isFetchNextPageError,
    error: submissionsError,
    refetch: refetchSubmissions,
    hasNextPage,
    isFetching: isFetchingSubmissions,
    isFetchingNextPage,
    fetchNextPage,
  } = useMySubmissions({
    subjectId: selectedSubjectId,
    status: selectedStatus,
    enabled: !isProposing,
  });

  const {
    data: sessionDetailData,
    isLoading: isLoadingDetail,
    error: detailError,
    refetch: refetchDetail,
  } = useSubmissionDetail(detailSessionId);

  const { updateMutation, deleteMutation } = useSessionMutations();

  // Flatten infinite query contents
  const allSubmissions = useMemo(() => {
    if (!submissionsPages?.pages) return [];
    return submissionsPages.pages.flatMap((page) => page?.contents || []);
  }, [submissionsPages]);

  // Handlers
  const handleOpenDetail = (sessionId: number) => {
    setDetailSessionId(sessionId);
  };

  const handleCloseDetail = () => {
    setDetailSessionId(null);
  };

  const handleOpenEditFromCard = (sub: SubmissionSessionSummaryDto) => {
    setDetailSessionId(sub.sessionId);
    // Detail data will load into sessionDetailData; when loaded, user can edit
  };

  const handleOpenEditFromDetail = (detail: MySubmissionDetailResponse) => {
    setEditDetail(detail);
    setDetailSessionId(null);
  };

  const handleSaveEdit = async (sessionId: number, data: UpdateSubmissionSessionRequest) => {
    await updateMutation.mutateAsync({ sessionId, data });
    setEditDetail(null);
  };

  const handleDeletePrompt = (sessionId: number, sessionCode: string) => {
    setDeleteTarget({ sessionId, sessionCode });
    setDetailSessionId(null);
  };

  const handleConfirmDelete = async (sessionId: number) => {
    await deleteMutation.mutateAsync(sessionId);
    setDeleteTarget(null);
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <StudyHeader
        section="Học tập"
        title={isProposing ? 'Đề xuất câu hỏi' : 'Ngân hàng câu hỏi'}
        description={isProposing
          ? 'Chọn lớp học phần, soạn câu hỏi và gửi để giảng viên xét duyệt.'
          : 'Theo dõi các phiên bạn đã nộp, xem kết quả duyệt và luyện tập với bộ câu hỏi của mình.'}
      />

      <StudyNavigation current={isProposing ? '/sessions/propose' : '/questions'} />

      {!isProposing && (
        <div className="space-y-6">
          {/* Filter Bar */}
          <SubmissionFilterBar
            subjects={subjectQuery.data ?? []}
            selectedSubjectId={selectedSubjectId}
            onSelectSubject={setSelectedSubjectId}
            selectedStatus={selectedStatus}
            onSelectStatus={setSelectedStatus}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
          />
          {subjectQuery.isError && (
            <div role="status" className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              Không tải được danh sách môn học.
              <Button variant="outline" onClick={() => void subjectQuery.refetch()} className="min-h-11">Thử lại</Button>
            </div>
          )}

          {/* Submissions List */}
          <SubmissionList
            submissions={allSubmissions}
            isLoading={isLoadingSubmissions}
            isError={isErrorSubmissions}
            errorMessage={submissionsError?.message}
            onRetry={() => void refetchSubmissions()}
            hasNextPage={Boolean(hasNextPage)}
            isFetching={isFetchingSubmissions}
            pageCount={submissionsPages?.pages.length ?? 0}
            isFetchingNextPage={isFetchingNextPage}
            isFetchNextPageError={isFetchNextPageError}
            selectedSubjectId={selectedSubjectId}
            onFetchNextPage={fetchNextPage}
            currentStatusFilter={selectedStatus}
            searchQuery={searchQuery}
            onViewDetail={handleOpenDetail}
            onEdit={handleOpenEditFromCard}
            onDelete={handleDeletePrompt}
            onProposeNew={() => setTab('propose')}
          />
        </div>
      )}

      {/* Tab 2: Propose Session Form Tab */}
      {isProposing && (
        <ProposeSessionForm
          onSuccessNavigate={() => setTab('submissions')}
          onViewMySubmissions={() => setTab('submissions')}
        />
      )}

      {/* Submission Detail Modal */}
      <SubmissionDetailModal
        detail={sessionDetailData}
        isLoading={isLoadingDetail}
        errorMessage={detailError?.message}
        onRetry={() => void refetchDetail()}
        isOpen={detailSessionId !== null}
        onClose={handleCloseDetail}
        onEdit={handleOpenEditFromDetail}
        onDelete={handleDeletePrompt}
      />

      {/* Edit Submission Modal */}
      <EditSubmissionModal
        detail={editDetail}
        isOpen={editDetail !== null}
        onClose={() => setEditDetail(null)}
        onSave={handleSaveEdit}
        isSaving={updateMutation.isPending}
      />

      {/* Delete Submission Dialog */}
      <DeleteSubmissionDialog
        isOpen={deleteTarget !== null}
        sessionId={deleteTarget?.sessionId ?? null}
        sessionCode={deleteTarget?.sessionCode}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        isDeleting={deleteMutation.isPending}
      />
    </div>
  );
}

export default SessionsPage;
