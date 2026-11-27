import type { ActivityFeedItemDto } from '@/features/feed/types/feed.types';
import type { PostResponseDto } from '@/types/post.types';

export function toFeedItem(post: PostResponseDto): ActivityFeedItemDto {
  return {
    feedId: post.postId,
    targetType: 'POST',
    targetId: post.postId,
    actionType: post.postType === 'LEARNING_VIDEO' ? 'PUBLISHED_LECTURE_VIDEO' : 'CREATED_POST',
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
      canEditLecturerNote: post.canEditLecturerNote,
      notedLecturer: post.notedLecturer,
      subjectName: post.subjectName,
      subjectCode: post.subjectCode,
      postType: post.postType,
      sessionId: post.sessionId,
    },
  };
}
