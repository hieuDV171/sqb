import { useState, useEffect } from 'react';
import { useActivityFeed, type FeedFilter } from '@/features/feed/hooks/useActivityFeed';
import { useNewFeedCount } from '@/features/feed/hooks/useNewFeedCount';
import { useLectureVideos } from '@/features/feed/hooks/useLectureVideos';
import { PullToRefreshFeed } from '@/features/feed/components/PullToRefreshFeed';
import { FeedFilterTabs } from '@/features/feed/components/FeedFilterTabs';
import { CreatePostBox } from '@/features/feed/components/CreatePostBox';
import { ActivityFeedRenderer } from '@/features/feed/components/ActivityFeedRenderer';
import { FeedCardSkeleton } from '@/features/feed/components/FeedCardSkeleton';
import { LectureVideoList } from '@/features/feed/components/LectureVideoList';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Button } from '@/components/ui/button';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { FeedSidebarRight } from './components/FeedSidebarRight';
import { sessionService } from '@/features/session/services/sessionService';
import type { SubjectItem } from '@/types/post.types';
import { MessageSquarePlus, Loader2, Sparkles } from 'lucide-react';
import { Navigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';

export function FeedRoute() {
  const [searchParams] = useSearchParams();
  const legacyPostId = Number(searchParams.get('postId'));

  if (Number.isSafeInteger(legacyPostId) && legacyPostId > 0) {
    return <Navigate to={`/posts/${legacyPostId}`} replace />;
  }

  return <FeedPage />;
}

export function FeedPage() {
  const [currentFilter, setCurrentFilter] = useState<FeedFilter>('ALL');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const role = useAuthStore((state) => state.user?.role);
  const showVideos = currentFilter === 'VIDEOS';
  const videos = useLectureVideos(selectedSubjectId, showVideos);

  const {
    items,
    data,
    isLoading,
    isError,
    isFetchNextPageError,
    refetch,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
    isFetching,
  } = useActivityFeed(currentFilter);

  const { newCount, triggerRefresh } = useNewFeedCount();

  useEffect(() => {
    let current = true;
    const loadSubjects = showVideos && (role === 'LECTURER' || role === 'ADMIN')
      ? sessionService.getAllSubjects : sessionService.getMyEnrolledSubjects;
    loadSubjects().then((res) => {
      if (current && res?.data) {
        setSubjects(
          res.data.map((s) => ({
            id: s.subjectId,
            code: s.code,
            name: s.name,
          }))
        );
      }
    }).catch(() => { if (current) setSubjects([]); });
    return () => { current = false; };
  }, [role, showVideos]);

  const handleRefreshAll = async () => {
    await triggerRefresh();
    if (showVideos) await videos.refetch();
    else await refetch();
  };

  // Additional local filter by subject if selected in right sidebar
  const displayedItems = selectedSubjectId
    ? items.filter((item) => {
        const selectedCode = subjects.find((s) => s.id === selectedSubjectId)?.code;
        return item.content.subjectCode === selectedCode;
      })
    : items;
  const canAutoLoad = !showVideos && displayedItems.length > 0 && hasNextPage && !isError && !isFetching;
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(canAutoLoad, fetchNextPage, data?.pages.length ?? 0);

  return (
    <div data-feed-page className="mx-auto max-w-6xl">
      <header className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold text-primary">
            <Sparkles className="size-4" aria-hidden="true" />
            Social Mode
          </div>
          <h1 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">Bảng tin học thuật</h1>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-muted-foreground">
            Theo dõi câu hỏi, tài liệu, bài giảng và thành tích mới nhất trong cộng đồng SQB.
          </p>
        </div>
      </header>
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* Main Feed Column (8 cols) */}
        <section aria-label="Dòng hoạt động" className="min-w-0 lg:col-span-8">
          {/* Create Post Card */}
          <CreatePostBox />

          {/* Dynamic Pull-to-Refresh & Floating Action Pill */}
          <PullToRefreshFeed onRefresh={handleRefreshAll} newCount={newCount}>
            {/* Feed Filter Tabs */}
            <FeedFilterTabs
              currentFilter={currentFilter}
              onFilterChange={setCurrentFilter}
              onManualRefresh={handleRefreshAll}
              isRefreshing={showVideos ? videos.isFetching : isLoading}
            />

            {showVideos ? (
              <LectureVideoList videos={videos} subjectId={selectedSubjectId} subjects={subjects} onSubjectChange={setSelectedSubjectId} />
            ) : <>
              {/* 4-State UI Stream */}
              {isLoading ? (
                <div className="space-y-4">
                  <FeedCardSkeleton />
                  <FeedCardSkeleton />
                  <FeedCardSkeleton />
                </div>
              ) : isError && !isFetchNextPageError ? (
                <ErrorState
                  title="Không thể tải dòng hoạt động"
                  message="Đã có lỗi xảy ra khi đồng bộ bảng tin từ máy chủ. Vui lòng thử lại."
                  onRetry={() => refetch()}
                />
              ) : displayedItems.length === 0 ? (
                <EmptyState
                  icon={MessageSquarePlus}
                  title="Chưa có hoạt động nào"
                  description={
                    hasNextPage
                      ? 'Chưa có hoạt động phù hợp trong các mục đã tải. Bạn có thể xem thêm hoạt động cũ hơn.'
                      : selectedSubjectId
                      ? 'Chưa có hoạt động hoặc bài viết nào cho môn học này.'
                      : 'Bảng tin hiện chưa có bài viết mới. Hãy là người đầu tiên chia sẻ câu hỏi hoặc tài liệu!'
                  }
                  actionLabel={selectedSubjectId ? 'Xem tất cả môn học' : undefined}
                  onAction={selectedSubjectId ? () => setSelectedSubjectId(undefined) : undefined}
                />
              ) : (
                <div className="space-y-4">
                  {displayedItems.map((item) => (
                    <ActivityFeedRenderer key={item.feedId} item={item} />
                  ))}

                </div>
              )}
              {hasNextPage && (!isError || isFetchNextPageError) && (
                <div ref={sentinel} className="grid min-h-px justify-items-center gap-2">
                  {isFetchNextPageError && <p role="alert" className="text-sm text-destructive">Không thể tải thêm hoạt động.</p>}
                  {isFetchingNextPage && (
                    <p role="status" className="flex items-center gap-2 py-4 text-sm text-muted-foreground">
                      <Loader2 className="size-4 motion-safe:animate-spin" aria-hidden="true" /> Đang tải thêm hoạt động…
                    </p>
                  )}
                  <Button variant="outline" onClick={() => void fetchNextPage({ cancelRefetch: false })} disabled={isFetching}
                    className={supportsAutoLoad && displayedItems.length > 0 && !isFetchNextPageError ? 'sr-only focus-visible:not-sr-only' : 'my-4 min-h-11'}>
                    {isFetchNextPageError ? 'Thử tải lại' : 'Xem thêm hoạt động cũ hơn'}
                  </Button>
                </div>
              )}
            </>}
          </PullToRefreshFeed>
        </section>

        {/* Right Sidebar Column (4 cols) */}
        <div className="sticky top-[calc(var(--app-header-height)+var(--space-4))] hidden lg:col-span-4 lg:block">
          <FeedSidebarRight
            subjects={subjects}
            selectedSubjectId={selectedSubjectId}
            onSelectSubject={setSelectedSubjectId}
          />
        </div>
      </div>
    </div>
  );
}
export default FeedPage;
