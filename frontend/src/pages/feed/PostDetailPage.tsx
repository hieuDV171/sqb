import { useEffect, useMemo, useRef } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Newspaper } from 'lucide-react';
import { ErrorState } from '@/components/ui/error-state';
import { FeedCardSkeleton } from '@/features/feed/components/FeedCardSkeleton';
import { PostFeedCard } from '@/features/feed/components/PostFeedCard';
import { usePostDetail } from '@/features/feed/hooks/usePostDetail';
import type { ActivityFeedItemDto } from '@/features/feed/types/feed.types';
import type { PostResponseDto } from '@/types/post.types';

function toFeedItem(post: PostResponseDto): ActivityFeedItemDto {
  return {
    feedId: post.postId,
    targetType: 'POST',
    targetId: post.postId,
    actionType: 'CREATED_POST',
    actor: post.author,
    createdAt: post.createdAt,
    content: {
      description: post.content,
      mediaUrls: post.mediaUrls,
      visibility: post.visibility,
      reactCount: post.reactCount,
      commentCount: post.commentCount,
      reactedByMe: post.reactedByMe,
      myReaction: post.myReaction,
      lecturerNote: post.lecturerNote,
      notedLecturer: post.notedLecturer,
      subject_name: post.subjectName,
      subject_code: post.subjectCode,
      postType: post.postType,
      sessionId: post.sessionId,
    },
  };
}

export function PostDetailPage() {
  const { postId: postIdParam } = useParams<{ postId: string }>();
  const parsedPostId = Number(postIdParam);
  const postId = Number.isSafeInteger(parsedPostId) && parsedPostId > 0 ? parsedPostId : null;
  const navigate = useNavigate();
  const location = useLocation();
  const pageRef = useRef<HTMLDivElement>(null);
  const { data: post, isLoading, isError, refetch } = usePostDetail(postId);

  const feedItem = useMemo(() => (post ? toFeedItem(post) : null), [post]);

  useEffect(() => {
    pageRef.current?.focus();
  }, []);

  const handleBack = () => {
    if (location.key !== 'default') {
      navigate(-1);
      return;
    }
    navigate('/feed', { replace: true });
  };

  return (
    <div
      ref={pageRef}
      tabIndex={-1}
      aria-labelledby="post-detail-heading"
      className="mx-auto w-full max-w-3xl outline-none"
    >
      <div className="mb-5 flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={handleBack}
          aria-label="Quay lại trang trước"
          className="inline-flex min-h-11 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-muted-foreground transition-colors hover:bg-card hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <ArrowLeft aria-hidden="true" className="h-5 w-5" />
          <span>Quay lại</span>
        </button>

        <div className="flex min-w-0 items-center gap-2 text-foreground">
          <Newspaper aria-hidden="true" className="h-5 w-5 shrink-0 text-primary" />
          <h1
            id="post-detail-heading"
            className="truncate text-base font-bold sm:text-lg"
          >
            Chi tiết bài viết
          </h1>
        </div>

        <Link
          to="/feed"
          className="inline-flex min-h-11 items-center rounded-xl px-3 text-sm font-semibold text-primary transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Bảng tin
        </Link>
      </div>

      {postId === null ? (
        <ErrorState
          title="Đường dẫn bài viết không hợp lệ"
          message="Mã bài viết trong đường dẫn không đúng. Hãy quay lại bảng tin và chọn lại bài viết."
        />
      ) : isLoading ? (
        <div aria-busy="true" aria-live="polite">
          <span className="sr-only">Đang tải chi tiết bài viết</span>
          <FeedCardSkeleton />
        </div>
      ) : isError || !feedItem ? (
        <ErrorState
          title="Không thể mở bài viết"
          message="Bài viết không tồn tại, đã bị xóa hoặc bạn không có quyền xem nội dung này."
          onRetry={() => refetch()}
        />
      ) : (
        <PostFeedCard
          item={feedItem}
          initiallyShowComments
          onDeleted={() => navigate('/feed', { replace: true })}
        />
      )}
    </div>
  );
}

export default PostDetailPage;
