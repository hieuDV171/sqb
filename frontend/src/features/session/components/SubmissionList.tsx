import React, { useMemo } from 'react';
import { RefreshCw, ArrowDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
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
  isFetchNextPageError: boolean;
  onFetchNextPage: () => void;
  selectedSubjectId?: number;
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
  isFetchNextPageError,
  onFetchNextPage,
  selectedSubjectId,
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
      <div className="grid gap-4 lg:grid-cols-2" aria-busy="true" aria-label="Đang tải các phiên nộp">
        <SubmissionCardSkeleton />
        <SubmissionCardSkeleton />
        <SubmissionCardSkeleton />
      </div>
    );
  }

  // Error State
  if (isError) {
    return (
      <ErrorState title="Không thể tải danh sách phiên nộp" message={errorMessage} onRetry={onRetry} />
    );
  }

  return (
    <div className="space-y-4">
      {filteredSubmissions.length === 0 ? (
        <EmptySubmissions
          filtered={Boolean(searchQuery.trim() || currentStatusFilter || selectedSubjectId)}
          hasNextPage={hasNextPage}
          onProposeNew={onProposeNew}
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {filteredSubmissions.map((sub) => (
            <SubmissionCard
              key={sub.sessionId}
              submission={sub}
              onViewDetail={onViewDetail}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}

      {hasNextPage && (
        <div className="space-y-2 text-center">
          {isFetchNextPageError && <p role="status" className="text-sm text-destructive">Không tải được trang tiếp theo.</p>}
          <Button
            variant="outline"
            onClick={onFetchNextPage}
            disabled={isFetchingNextPage}
            className="min-h-11"
          >
            {isFetchingNextPage ? (
              <>
                <RefreshCw aria-hidden="true" className="motion-safe:animate-spin" />
                <span>Đang tải thêm phiên...</span>
              </>
            ) : (
              <>
                <ArrowDown aria-hidden="true" />
                <span>{isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm phiên'}</span>
              </>
            )}
          </Button>
        </div>
      )}
    </div>
  );
};
