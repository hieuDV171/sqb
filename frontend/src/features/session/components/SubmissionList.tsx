import React, { useMemo } from 'react';
import { RefreshCw, AlertCircle, ArrowDown } from 'lucide-react';
import { SubmissionCard } from './SubmissionCard';
import { SubmissionCardSkeleton } from './SubmissionCardSkeleton';
import { EmptySubmissions } from './EmptySubmissions';
import type {
  SubmissionSessionSummaryDto,
  SessionStatus,
} from '../types/session.types';

interface SubmissionListProps {
  submissions: SubmissionSessionSummaryDto[];
  isLoading: boolean;
  isError: boolean;
  errorMessage?: string;
  onRetry: () => void;
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  onFetchNextPage: () => void;
  currentStatusFilter?: SessionStatus;
  searchQuery: string;
  onViewDetail: (sessionId: number) => void;
  onEdit?: (submission: SubmissionSessionSummaryDto) => void;
  onDelete?: (sessionId: number, sessionCode: string) => void;
  onProposeNew?: () => void;
}

export const SubmissionList: React.FC<SubmissionListProps> = ({
  submissions,
  isLoading,
  isError,
  errorMessage,
  onRetry,
  hasNextPage,
  isFetchingNextPage,
  onFetchNextPage,
  currentStatusFilter,
  searchQuery,
  onViewDetail,
  onEdit,
  onDelete,
  onProposeNew,
}) => {
  // Filter submissions by search query
  const filteredSubmissions = useMemo(() => {
    if (!searchQuery.trim()) return submissions;
    const q = searchQuery.toLowerCase().trim();
    return submissions.filter(
      (s) =>
        (s.title && s.title.toLowerCase().includes(q)) ||
        (s.sessionCode && s.sessionCode.toLowerCase().includes(q)) ||
        (s.subjectName && s.subjectName.toLowerCase().includes(q)) ||
        (s.subjectCode && s.subjectCode.toLowerCase().includes(q)) ||
        (s.content && s.content.toLowerCase().includes(q))
    );
  }, [submissions, searchQuery]);

  // Loading State
  if (isLoading) {
    return (
      <div className="space-y-4">
        <SubmissionCardSkeleton />
        <SubmissionCardSkeleton />
        <SubmissionCardSkeleton />
      </div>
    );
  }

  // Error State
  if (isError) {
    return (
      <div className="bg-rose-50/70 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-900/50 rounded-3xl p-8 text-center space-y-3">
        <div className="w-12 h-12 rounded-2xl bg-rose-100 dark:bg-rose-900/40 text-rose-600 dark:text-rose-400 flex items-center justify-center mx-auto">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h4 className="text-base font-bold text-rose-900 dark:text-rose-200">
          Không thể tải danh sách phiên nộp
        </h4>
        <p className="text-xs text-rose-700 dark:text-rose-400 max-w-md mx-auto">
          {errorMessage || 'Đã có lỗi xảy ra trong quá trình truy xuất dữ liệu từ máy chủ.'}
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Thử lại</span>
        </button>
      </div>
    );
  }

  // Empty State
  if (filteredSubmissions.length === 0) {
    return (
      <EmptySubmissions
        filtered={Boolean(searchQuery.trim() || currentStatusFilter)}
        onProposeNew={onProposeNew}
      />
    );
  }

  // Success List
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4">
        {filteredSubmissions.map((sub) => (
          <SubmissionCard
            key={sub.sessionId}
            submission={sub}
            currentStatusFilter={currentStatusFilter}
            onViewDetail={onViewDetail}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        ))}
      </div>

      {/* Infinite Scroll / Load More trigger */}
      {hasNextPage && (
        <div className="flex justify-center pt-4 pb-2">
          <button
            type="button"
            onClick={onFetchNextPage}
            disabled={isFetchingNextPage}
            className="flex items-center gap-2 px-6 py-2.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 text-xs sm:text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50"
          >
            {isFetchingNextPage ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin text-indigo-600" />
                <span>Đang tải thêm phiên...</span>
              </>
            ) : (
              <>
                <ArrowDown className="w-4 h-4 text-indigo-600" />
                <span>Tải thêm phiên cũ hơn</span>
              </>
            )}
          </button>
        </div>
      )}
    </div>
  );
};
