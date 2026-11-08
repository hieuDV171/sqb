import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, BookOpen, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useUserProposedSessions, UserSessionCard } from '@/features/practice';
import { useEnrolledSubjects } from '@/features/session';
import { StudySubjectSelect } from '@/features/session/components/StudySubjectSelect';
import { StudyHeader } from './StudyHeader';

export function UserSessionsPage() {
  const { userId } = useParams<{ userId: string }>();
  const numericUserId = Number(userId);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number>();
  const [searchQuery, setSearchQuery] = useState('');
  const subjects = useEnrolledSubjects();
  const sessions = useUserProposedSessions(numericUserId, { subject_id: selectedSubjectId });
  const query = searchQuery.trim().toLocaleLowerCase('vi-VN');
  const loaded = sessions.data?.pages.flatMap((page) => page.contents ?? []) ?? [];
  const filtered = loaded.filter(({ title, sessionCode, subjectName }) => (
    [title, sessionCode, subjectName].some((value) => value?.toLocaleLowerCase('vi-VN').includes(query))
  ));
  const fieldClass = 'min-h-11 w-full min-w-0 rounded-xl border border-input bg-background px-3 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring';

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <Button variant="ghost" className="min-h-11" render={<Link to={Number.isSafeInteger(numericUserId) && numericUserId > 0 ? `/users/${numericUserId}` : '/questions'} />} nativeButton={false}>
        <ArrowLeft aria-hidden="true" /> Về hồ sơ
      </Button>
      <StudyHeader section="Học tập" title="Bộ câu hỏi của thành viên" description="Chọn một bộ câu hỏi để luyện tập." />

      <section aria-label="Lọc bộ câu hỏi" className="space-y-3 rounded-2xl border border-border bg-card p-4 sm:p-5">
        <div className="grid gap-3 sm:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
          <StudySubjectSelect subjects={subjects.data ?? []} value={selectedSubjectId} onChange={setSelectedSubjectId} />
          <label className="min-w-0 space-y-1.5 text-sm font-medium">
            <span className="block">Tìm trong các bộ đã tải</span>
            <input className={fieldClass} type="search" value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)} placeholder="Tiêu đề hoặc mã phiên" />
          </label>
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
          {filtered.length ? (
            <div className="grid gap-4 md:grid-cols-2">
              {filtered.map((session) => <UserSessionCard key={session.sessionId} session={session} />)}
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card p-8 text-center">
              <BookOpen aria-hidden="true" className="mx-auto mb-3 size-7 text-academic" />
              <h2 className="font-semibold">{query || selectedSubjectId ? 'Chưa tìm thấy bộ câu hỏi phù hợp' : 'Chưa có bộ câu hỏi'}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {sessions.hasNextPage ? 'Bạn có thể tải thêm để tiếp tục tìm.' : 'Các bộ câu hỏi có thể truy cập sẽ xuất hiện ở đây.'}
              </p>
            </div>
          )}
          {sessions.hasNextPage && (
            <div className="space-y-2 text-center">
              {sessions.isFetchNextPageError && <p role="status" className="text-sm text-destructive">Không tải được trang tiếp theo.</p>}
              <Button variant="outline" className="min-h-11" disabled={sessions.isFetchingNextPage} onClick={() => void sessions.fetchNextPage()}>
                {sessions.isFetchingNextPage && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
                {sessions.isFetchingNextPage ? 'Đang tải…' : sessions.isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm bộ câu hỏi'}
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
