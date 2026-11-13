import { Link } from 'react-router-dom';
import { Video } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import { StudySubjectSelect } from '@/features/session/components/StudySubjectSelect';
import type { SubjectItem } from '@/types/post.types';
import type { useLectureVideos } from '../hooks/useLectureVideos';
import { FeedCardSkeleton } from './FeedCardSkeleton';

export function LectureVideoList({ videos, subjectId, subjects, onSubjectChange }: {
  videos: ReturnType<typeof useLectureVideos>;
  subjectId?: number;
  subjects: SubjectItem[];
  onSubjectChange: (subjectId?: number) => void;
}) {
  const items = videos.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <section aria-label="Video bài giảng" className="space-y-4">
      <div className="rounded-xl border border-border bg-card p-3">
        <StudySubjectSelect subjects={subjects.map(({ id, code, name }) => ({ subjectId: id, code, name }))}
          value={subjectId} onChange={onSubjectChange} />
      </div>
      {videos.isPending ? <FeedCardSkeleton /> : videos.isLoadingError ? (
        <ErrorState title="Không thể tải video bài giảng" onRetry={() => void videos.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={Video} title="Chưa có video bài giảng" />
      ) : items.map((item) => {
        const media = item.mediaUrls?.find(({ mediaType }) => mediaType.toLowerCase().startsWith('video'));
        return (
          <article key={item.postId} className="space-y-3 rounded-2xl border border-border bg-card p-4">
            <p className="text-sm text-muted-foreground">{item.subjectName} · {item.author.fullName}</p>
            <Link to={`/posts/${item.postId}`} className="block whitespace-pre-wrap font-semibold hover:underline">
              {item.content || 'Video bài giảng'}
            </Link>
            {media && <video controls preload="metadata" src={media.url} poster={media.thumbnailUrl}
              aria-label={item.content || 'Video bài giảng'} className="aspect-video w-full rounded-xl bg-black" />}
          </article>
        );
      })}
      <InfiniteListFooter query={videos} label="Tải thêm video" />
    </section>
  );
}
