import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { BookOpen, Loader2, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { usePracticeCatalog, PracticeSetCard } from '@/features/practice';
import { sessionService } from '@/features/session/services/sessionService';
import { StudySubjectSelect } from '@/features/session/components/StudySubjectSelect';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { StudyHeader } from './StudyHeader';
import { StudyNavigation } from './StudyNavigation';

export function PracticeCatalogPage() {
  const [params, setParams] = useSearchParams();
  const subjectId = Number(params.get('subject'));
  const selectedSubjectId = Number.isSafeInteger(subjectId) && subjectId > 0 ? subjectId : undefined;
  const query = params.get('q')?.slice(0, 120).trim() ?? '';
  const subjects = useQuery({
    queryKey: ['subjects', 'practice-catalog'],
    queryFn: async () => (await sessionService.getAllSubjects()).data ?? [],
    staleTime: 5 * 60_000,
  });
  const sessions = usePracticeCatalog(selectedSubjectId, query);
  const sets = sessions.data?.pages.flatMap((page) => page.contents) ?? [];
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    sessions.hasNextPage && !sessions.isFetching && !sessions.isFetchNextPageError,
    sessions.fetchNextPage,
    sessions.data?.pages.length ?? 0,
  );

  function setFilter(name: 'subject' | 'q', value?: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(name, value);
    else next.delete(name);
    setParams(next);
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <StudyHeader section="Ngân hàng câu hỏi" title="Kho luyện tập" description="Tìm bộ câu hỏi theo môn học, luyện tập và thảo luận cùng cộng đồng." />
      <StudyNavigation current="/questions/practice" />

      <section aria-label="Lọc bộ câu hỏi" className="space-y-3 rounded-2xl border border-border bg-card p-4 sm:p-5">
        <div className="grid gap-3 sm:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
          <StudySubjectSelect subjects={subjects.data ?? []} value={selectedSubjectId}
            onChange={(value) => setFilter('subject', value?.toString())} />
          <form className="flex min-w-0 items-end gap-2" onSubmit={(event) => {
            event.preventDefault();
            setFilter('q', String(new FormData(event.currentTarget).get('q') ?? '').trim());
          }}>
            <label className="min-w-0 flex-1 space-y-1.5 text-sm font-medium">
              <span className="block">Tìm bộ câu hỏi</span>
              <input key={query} name="q" type="search" defaultValue={query} maxLength={120}
                className="min-h-11 w-full min-w-0 rounded-xl border border-input bg-background px-3 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
                placeholder="Tiêu đề hoặc mã phiên" />
            </label>
            <Button type="submit" variant="outline" className="min-h-11"><Search aria-hidden="true" /> Tìm</Button>
          </form>
        </div>
        {subjects.isError && (
          <p className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
            Không tải được danh sách môn học.
            <Button variant="outline" className="min-h-11" onClick={() => void subjects.refetch()}>Thử lại</Button>
          </p>
        )}
      </section>

      {sessions.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2" aria-busy="true" aria-label="Đang tải các bộ câu hỏi">
          {[0, 1, 2, 3].map((index) => <Skeleton key={index} className="h-48 rounded-2xl border border-border motion-reduce:animate-none" />)}
        </div>
      ) : sessions.isLoadingError ? (
        <ErrorState title="Không thể tải danh sách bộ câu hỏi" message={sessions.error?.message} onRetry={() => void sessions.refetch()} />
      ) : (
        <>
          {sets.length ? (
            <div className="grid gap-4 md:grid-cols-2">
              {sets.map((session) => <PracticeSetCard key={session.sessionId} session={session} catalogSearch={params.toString()} />)}
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card p-8 text-center">
              <BookOpen aria-hidden="true" className="mx-auto mb-3 size-7 text-academic" />
              <h2 className="font-semibold">{query || selectedSubjectId ? 'Chưa tìm thấy bộ câu hỏi phù hợp' : 'Chưa có bộ câu hỏi'}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {query || selectedSubjectId ? 'Thử một từ khóa hoặc chọn môn học khác.' : 'Các bộ câu hỏi từ phiên đã chốt sẽ xuất hiện ở đây.'}
              </p>
            </div>
          )}
          {sessions.hasNextPage && (
            <div className="space-y-2 text-center">
              <div ref={sentinel} aria-hidden="true" className="h-px" />
              {sessions.isFetchNextPageError && <p role="status" className="text-sm text-destructive">Không tải được trang tiếp theo.</p>}
              {sessions.isFetchingNextPage ? (
                <p role="status" className="inline-flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 aria-hidden="true" className="size-4 motion-safe:animate-spin" /> Đang tải thêm bộ câu hỏi…
                </p>
              ) : (!supportsAutoLoad || sessions.isFetchNextPageError) && (
                <Button variant="outline" className="min-h-11" disabled={sessions.isFetching}
                  onClick={() => void sessions.fetchNextPage({ cancelRefetch: false })}>
                  {sessions.isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm bộ câu hỏi'}
                </Button>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
