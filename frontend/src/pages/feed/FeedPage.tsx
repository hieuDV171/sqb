import { useState, useEffect } from 'react';
import { useActivityFeed, type FeedFilter } from '@/features/feed/hooks/useActivityFeed';
import { useNewFeedCount } from '@/features/feed/hooks/useNewFeedCount';
import { PullToRefreshFeed } from '@/features/feed/components/PullToRefreshFeed';
import { FeedFilterTabs } from '@/features/feed/components/FeedFilterTabs';
import { CreatePostBox } from '@/features/feed/components/CreatePostBox';
import { ActivityFeedRenderer } from '@/features/feed/components/ActivityFeedRenderer';
import { FeedCardSkeleton } from '@/features/feed/components/FeedCardSkeleton';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { FeedSidebarRight } from './components/FeedSidebarRight';
import { sessionService } from '@/features/session/services/sessionService';
import type { SubjectItem } from '@/types/post.types';
import { MessageSquarePlus, Loader2 } from 'lucide-react';

export function FeedPage() {
  const [currentFilter, setCurrentFilter] = useState<FeedFilter>('ALL');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);

  const {
    items,
    isLoading,
    isError,
    refetch,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
  } = useActivityFeed(currentFilter);

  const { newCount, triggerRefresh } = useNewFeedCount();

  useEffect(() => {
    sessionService.getMyEnrolledSubjects().then((res) => {
      if (res?.data) {
        setSubjects(
          res.data.map((s) => ({
            id: s.subjectId,
            code: s.code,
            name: s.name,
            postCount: 12,
          }))
        );
      }
    });
  }, []);

  const handleRefreshAll = async () => {
    await triggerRefresh();
    await refetch();
  };

  // Additional local filter by subject if selected in right sidebar
  const displayedItems = selectedSubjectId
    ? items.filter((item) => {
        const selectedCode = subjects.find((s) => s.id === selectedSubjectId)?.code;
        return item.content.subject_code === selectedCode;
      })
    : items;

  return (
    <div className="max-w-6xl mx-auto">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Main Feed Column (8 cols) */}
        <div className="lg:col-span-8">
          {/* Create Post Card */}
          <CreatePostBox />

          {/* Dynamic Pull-to-Refresh & Floating Action Pill */}
          <PullToRefreshFeed onRefresh={handleRefreshAll} newCount={newCount}>
            {/* Feed Filter Tabs */}
            <FeedFilterTabs
              currentFilter={currentFilter}
              onFilterChange={setCurrentFilter}
              onManualRefresh={handleRefreshAll}
              isRefreshing={isLoading}
            />

            {/* 4-State UI Stream */}
            {isLoading ? (
              <div className="space-y-4">
                <FeedCardSkeleton />
                <FeedCardSkeleton />
                <FeedCardSkeleton />
              </div>
            ) : isError ? (
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
                  selectedSubjectId
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

                {/* Infinite Scroll / Load More Action */}
                {hasNextPage && (
                  <div className="flex justify-center pt-2 pb-6">
                    <button
                      type="button"
                      onClick={() => fetchNextPage()}
                      disabled={isFetchingNextPage}
                      className="flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-xs md:text-sm font-semibold text-slate-700 dark:text-slate-200 hover:border-blue-500 hover:text-blue-600 dark:hover:text-blue-400 shadow-xs hover:shadow-md transition-all cursor-pointer disabled:opacity-50"
                    >
                      {isFetchingNextPage ? (
                        <>
                          <Loader2 className="w-4 h-4 animate-spin text-blue-500" />
                          <span>Đang tải các hoạt động cũ hơn...</span>
                        </>
                      ) : (
                        <span>Xem thêm hoạt động cũ hơn</span>
                      )}
                    </button>
                  </div>
                )}
              </div>
            )}
          </PullToRefreshFeed>
        </div>

        {/* Right Sidebar Column (4 cols) */}
        <div className="hidden lg:block lg:col-span-4 sticky top-20">
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
