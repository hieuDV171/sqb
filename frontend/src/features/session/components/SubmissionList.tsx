import type { FC } from 'react';
import { RefreshCw, ArrowDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
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
  isFetching: boolean;
  pageCount: number;
  isFetchingNextPage: boolean;
  isFetchNextPageError: boolean;
  onFetchNextPage: (options: { cancelRefetch: boolean }) => Promise<unknown>;
  selectedSubjectId?: number;
  currentStatusFilter?: SessionStatus;
  searchQuery: string;
  onViewDetail: (sessionId: number) => void;
  onEdit?: (submission: SubmissionSessionSummaryDto) => void;
  onDelete?: (sessionId: number, sessionCode: string) => void;
  onProposeNew?: () => void;
}

export const SubmissionList: FC<SubmissionListProps> = ({
  submissions,
  isLoading,
  isError,
  errorMessage,
  onRetry,
  hasNextPage,
  isFetching,
  pageCount,
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
  const hasVisibleSubmissions = submissions.length > 0;
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    hasNextPage && !isFetching && !isError && !isFetchNextPageError && hasVisibleSubmissions,
    onFetchNextPage,
    pageCount,
  );

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
      {!hasVisibleSubmissions ? (
        <EmptySubmissions
          filtered={Boolean(searchQuery.trim() || currentStatusFilter || selectedSubjectId)}
          hasNextPage={hasNextPage}
          onProposeNew={onProposeNew}
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {submissions.map((sub) => (
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
          <div ref={sentinel} aria-hidden="true" className="h-px" />
          {isFetchNextPageError && <p role="status" className="text-sm text-destructive">Không tải được trang tiếp theo.</p>}
          {isFetchingNextPage ? (
            <p role="status" className="inline-flex items-center gap-2 text-sm text-muted-foreground">
              <RefreshCw aria-hidden="true" className="size-4 motion-safe:animate-spin" /> Đang tải thêm phiên...
            </p>
          ) : (!supportsAutoLoad || isFetchNextPageError || !hasVisibleSubmissions) && (
            <Button variant="outline" onClick={() => void onFetchNextPage({ cancelRefetch: false })} disabled={isFetching} className="min-h-11">
              <ArrowDown aria-hidden="true" /> {isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm phiên'}
            </Button>
          )}
        </div>
      )}
    </div>
  );
};
