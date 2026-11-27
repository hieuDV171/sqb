import { Ban, Newspaper } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { FeedCardSkeleton } from '@/features/feed/components/FeedCardSkeleton';
import { PostFeedCard } from '@/features/feed/components/PostFeedCard';
import { toFeedItem } from '@/features/feed/utils/toFeedItem';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { isProfilePostsBlocked, useProfilePosts } from '../hooks/useProfilePosts';

export function ProfilePosts({ userId, isOwnProfile }: { userId: number; isOwnProfile: boolean }) {
  const query = useProfilePosts(userId, isOwnProfile);
  const items = query.data?.pages.flatMap((page) => page.items) ?? [];
  const checkingAccess = !isOwnProfile && !query.isFetchedAfterMount && query.isFetching;
  const blocked = isProfilePostsBlocked(query.error);
  const { fetchNextPage, hasNextPage, isFetching, isError } = query;
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    !checkingAccess && !blocked && hasNextPage && !isFetching && !isError,
    fetchNextPage,
    query.data?.pages.length ?? 0,
  );

  return (
    <section aria-labelledby="profile-posts-heading" className="space-y-4">
      <h2 id="profile-posts-heading" className="flex items-center gap-2 text-xl font-semibold tracking-tight">
        <Newspaper className="size-5 text-primary" aria-hidden="true" /> Bài viết
      </h2>
      {blocked ? (
        <div role="status" className="grid min-h-48 place-content-center justify-items-center gap-3 rounded-2xl border border-border bg-card p-6 text-center">
          <span className="grid size-14 place-items-center rounded-full bg-muted text-muted-foreground">
            <Ban className="size-7" aria-hidden="true" />
          </span>
          <p className="font-semibold">Bạn không thể xem bài viết của người này.</p>
        </div>
      ) : query.isPending || checkingAccess ? (
        <div role="status" aria-label="Đang tải bài viết"><FeedCardSkeleton /></div>
      ) : query.isError && !query.isFetchNextPageError ? (
        <ErrorState title="Không thể tải bài viết" onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={Newspaper} title={isOwnProfile ? 'Bạn chưa có bài viết' : 'Chưa có bài viết để hiển thị'} />
      ) : (
        <>
          {items.map((post) => <PostFeedCard key={post.postId} item={toFeedItem(post)} />)}
          {query.hasNextPage && (
            <div ref={sentinel} className="flex min-h-px flex-col items-center gap-2">
              {query.isFetchNextPageError && <p role="alert" className="text-sm text-destructive">Không thể tải thêm bài viết.</p>}
              {query.isFetchingNextPage && <p role="status" className="py-2 text-sm text-muted-foreground">Đang tải thêm bài viết…</p>}
              <Button variant="outline"
                className={supportsAutoLoad && !query.isFetchNextPageError ? 'sr-only focus-visible:not-sr-only' : undefined}
                disabled={query.isFetching} onClick={() => void query.fetchNextPage({ cancelRefetch: false })}>
                {query.isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm bài viết'}
              </Button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
