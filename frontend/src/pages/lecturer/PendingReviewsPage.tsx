import { CursorPagination, type PageCursor } from '@/components/ui/cursor-pagination';
import { useSearchParams } from 'react-router-dom';
import { ClipboardCheck, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useCourseClasses } from '@/features/course-class/hooks/useCourseClasses';
import { PendingSessionCard } from '@/features/review/components/PendingSessionCard';
import { PendingSessionFilterBar } from '@/features/review/components/PendingSessionFilterBar';
import { useReviewableSessions } from '@/features/review/hooks/useReviewableSessions';
import { useAuthStore } from '@/stores/useAuthStore';

export function PendingReviewsPage() {
  const [params, setParams] = useSearchParams();
  const isAdmin = useAuthStore((state) => state.user?.role === 'ADMIN');
  const classes = useCourseClasses(isAdmin);
  const classId = Number(params.get('classId'));
  const selectedClassId = Number.isSafeInteger(classId) && classId > 0 ? classId : undefined;
  const search = params.get('q') ?? '';
  const readCursor = (key: 'after' | 'before') => {
    const value = Number(params.get(key));
    return Number.isSafeInteger(value) && value > 0 ? value : undefined;
  };
  const before = readCursor('before');
  const cursor: PageCursor = before === undefined ? { after: readCursor('after') } : { before };
  const sessions = useReviewableSessions({ course_class_id: selectedClassId, q: search.trim(), ...cursor });
  const visible = sessions.data?.items ?? [];

  function changePage(page: PageCursor) {
    const next = new URLSearchParams(params);
    next.delete('before');
    next.delete('after');
    if (page.before !== undefined) next.set('before', String(page.before));
    if (page.after !== undefined) next.set('after', String(page.after));
    setParams(next);
  }

  function updateFilter(key: 'q' | 'classId', value?: string) {
    const next = new URLSearchParams(params);
    next.delete('before');
    next.delete('after');
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5 pb-10">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-5">
        <div className="space-y-2">
          <p className="flex items-center gap-2 text-sm font-semibold text-academic">
            <ClipboardCheck aria-hidden="true" className="size-4" /> Không gian giảng viên
          </p>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Phiên cần duyệt</h1>
          <p className="text-sm text-muted-foreground">Xét duyệt câu hỏi sinh viên đề xuất theo lớp học phần.</p>
        </div>
        <Button variant="outline" className="min-h-11" onClick={() => void sessions.refetch()} disabled={sessions.isFetching}>
          <RefreshCw aria-hidden="true" /> Làm mới
        </Button>
      </header>

      <PendingSessionFilterBar
        search={search}
        onSearchChange={(value) => updateFilter('q', value)}
        selectedClassId={selectedClassId}
        onClassChange={(id) => updateFilter('classId', id?.toString())}
        courseClasses={classes.data ?? []}
        isLoadingClasses={classes.isPending}
      />
      {classes.isError && (
        <p className="flex flex-wrap items-center gap-2 text-sm text-destructive" role="status">
          Không tải được bộ lọc lớp học phần.
          <Button variant="ghost" className="min-h-11" onClick={() => void classes.refetch()}>Thử lại</Button>
        </p>
      )}

      {sessions.isPending ? (
        <div role="status" aria-label="Đang tải phiên cần duyệt" className="space-y-3">
          {[1, 2, 3].map((key) => <Skeleton key={key} className="h-36 rounded-2xl" />)}
        </div>
      ) : !sessions.data ? (
        <ErrorState title="Không thể tải phiên cần duyệt" onRetry={() => void sessions.refetch()} />
      ) : (
        <section aria-label="Danh sách phiên cần duyệt" className="space-y-3">
          {sessions.isRefetchError && (
            <p role="status" className="text-sm text-destructive">Không tải được danh sách mới nhất. Dùng nút Làm mới để thử lại.</p>
          )}
          {visible.length === 0 ? (
            <EmptyState
              title={search.trim() ? 'Không tìm thấy phiên phù hợp' : 'Chưa có phiên cần duyệt'}
              description="Thử đổi từ khóa hoặc chọn lớp học phần khác."
            />
          ) : (
            visible.map((session) => (
              <PendingSessionCard key={session.sessionId} session={session} queueSearch={params.toString()} />
            ))
          )}
        </section>
      )}
      <CursorPagination pagination={sessions.data?.pagination} cursor={cursor}
        busy={sessions.isFetching} onChange={changePage} />
    </div>
  );
}
