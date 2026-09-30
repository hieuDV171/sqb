import type { ActivityFeedItemDto } from '@/types/activityFeed.types';
import type { PostResponseDto } from '@/types/post.types';
import { PostCard } from './PostCard';
import { LectureVideoCard } from './cards/LectureVideoCard';
import { ApprovedSessionCard } from './cards/ApprovedSessionCard';
import { EarnedBadgeCard } from './cards/EarnedBadgeCard';
import { ReachedMilestoneCard } from './cards/ReachedMilestoneCard';

interface ActivityFeedItemRendererProps {
  item: ActivityFeedItemDto;
  onToggleLike: (postId: number) => void;
  onAddComment: (postId: number, content: string, parentCommentId?: number) => void;
}

export function ActivityFeedItemRenderer({
  item,
  onToggleLike,
  onAddComment,
}: ActivityFeedItemRendererProps) {
  switch (item.actionType) {
    case 'PUBLISHED_LECTURE_VIDEO':
      return <LectureVideoCard item={item} />;

    case 'SESSION_RESOLVED_APPROVED':
      return <ApprovedSessionCard item={item} />;

    case 'EARNED_BADGE':
      return <EarnedBadgeCard item={item} />;

    case 'REACHED_MILESTONE':
      return <ReachedMilestoneCard item={item} />;

    case 'CREATED_POST':
    default: {
      // Map ActivityFeedItemDto sang PostResponseDto cho PostCard
      const postDto: PostResponseDto = {
        postId: item.targetId || item.feedId,
        content: item.content.description || item.content.title || '',
        postType: 'SOCIAL_POST',
        visibility: item.content.visibility || 'PUBLIC',
        author: item.actor,
        reactCount: item.content.reactCount || 18,
        commentCount: item.content.commentCount || 4,
        reactedByMe: item.content.reactedByMe || false,
        createdAt: item.createdAt,
        lecturerNote: item.content.lecturerNote,
        notedLecturer: item.content.notedLecturer,
        subjectCode: item.content.subject_code,
        subjectName: item.content.subject_name,
        mediaUrls: item.content.media_url
          ? [
              {
                objectKey: 'media/post.jpg',
                url: item.content.media_url,
                mediaType: 'image/jpeg',
              },
            ]
          : undefined,
      };

      return (
        <PostCard
          post={postDto}
          onToggleLike={onToggleLike}
          onAddComment={onAddComment}
        />
      );
    }
  }
}
