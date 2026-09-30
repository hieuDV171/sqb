import { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  BookOpen,
  Plus,
  ListOrdered,
  Sparkles,
} from 'lucide-react';
import { cn } from '@/lib/utils';
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
} from '@/features/session';

export function SessionsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') === 'propose' ? 'propose' : 'submissions';

  const setTab = (tab: 'submissions' | 'propose') => {
    setSearchParams(tab === 'propose' ? { tab: 'propose' } : {});
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
  const { data: subjects = [] } = useEnrolledSubjects();

  const {
    data: submissionsPages,
    isLoading: isLoadingSubmissions,
    isError: isErrorSubmissions,
    error: submissionsError,
    refetch: refetchSubmissions,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useMySubmissions({
    subjectId: selectedSubjectId,
    status: selectedStatus,
  });

  const {
    data: sessionDetailData,
    isLoading: isLoadingDetail,
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

  const handleOpenEditFromCard = async (sub: SubmissionSessionSummaryDto) => {
    setDetailSessionId(sub.sessionId);
    // Detail data will load into sessionDetailData; when loaded, user can edit
  };

  const handleOpenEditFromDetail = (detail: MySubmissionDetailResponse) => {
    setEditDetail(detail);
    setDetailSessionId(null);
  };

  const handleSaveEdit = async (sessionId: number, data: any) => {
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
    <div className="max-w-6xl mx-auto px-3 sm:px-6 py-6 sm:py-8 space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200/80 dark:border-slate-800/80 pb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-2 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <BookOpen className="w-6 h-6" />
            </span>
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white tracking-tight">
              Ngân Hàng Câu Hỏi & Đề Xuất
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
            Đóng góp câu hỏi trắc nghiệm, theo dõi xét duyệt và tích lũy điểm thưởng chuyên môn
          </p>
        </div>

        {/* Tab Switcher Pills */}
        <div className="flex items-center p-1.5 bg-slate-100/90 dark:bg-slate-800/90 border border-slate-200/60 dark:border-slate-700/60 rounded-2xl self-start sm:self-auto shadow-xs">
          <button
            type="button"
            onClick={() => setTab('submissions')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer',
              activeTab === 'submissions'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            )}
          >
            <ListOrdered className="w-4 h-4 text-indigo-600" />
            <span>Phiên nộp của tôi</span>
            {allSubmissions.length > 0 && (
              <span className="px-2 py-0.5 rounded-full text-[11px] bg-indigo-100 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 font-bold">
                {allSubmissions.length}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setTab('propose')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer',
              activeTab === 'propose'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            )}
          >
            <Plus className="w-4 h-4 text-indigo-600" />
            <span>Đề xuất phiên mới</span>
            <Sparkles className="w-3.5 h-3.5 text-amber-500" />
          </button>
        </div>
      </div>

      {/* Tab 1: Submissions Tab */}
      {activeTab === 'submissions' && (
        <div className="space-y-6">
          {/* Filter Bar */}
          <SubmissionFilterBar
            subjects={subjects}
            selectedSubjectId={selectedSubjectId}
            onSelectSubject={setSelectedSubjectId}
            selectedStatus={selectedStatus}
            onSelectStatus={setSelectedStatus}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            onProposeNew={() => setTab('propose')}
          />

          {/* Submissions List */}
          <SubmissionList
            submissions={allSubmissions}
            isLoading={isLoadingSubmissions}
            isError={isErrorSubmissions}
            errorMessage={submissionsError?.message}
            onRetry={refetchSubmissions}
            hasNextPage={Boolean(hasNextPage)}
            isFetchingNextPage={isFetchingNextPage}
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
      {activeTab === 'propose' && (
        <ProposeSessionForm
          onSuccessNavigate={() => setTab('submissions')}
          onViewMySubmissions={() => setTab('submissions')}
        />
      )}

      {/* Submission Detail Modal */}
      <SubmissionDetailModal
        detail={sessionDetailData}
        isLoading={isLoadingDetail}
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
