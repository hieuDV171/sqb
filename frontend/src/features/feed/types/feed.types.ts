import type { AuthorDto, MediaItem, PostType, PostVisibility } from '@/types/post.types';

export type ActionType =
  | 'CREATED_POST'
  | 'PUBLISHED_LECTURE_VIDEO'
  | 'SESSION_RESOLVED_APPROVED'
  | 'EARNED_BADGE'
  | 'REACHED_MILESTONE';

export interface ActivityFeedContent {
  title?: string;
  description?: string;
  media_url?: string;
  badge_name?: string;
  badge_icon_url?: string;
  cosmetic_name?: string;
  subject_code?: string;
  subject_name?: string;
  visibility?: PostVisibility;
  duration?: string;
  reactCount?: number;
  commentCount?: number;
  reactedByMe?: boolean;
  lecturerNote?: string;
  notedLecturer?: AuthorDto;
  sessionId?: number;
  pointsEarned?: number;
  streakCount?: number;
  // Metadata cho bài viết
  mediaUrls?: MediaItem[];
  postType?: PostType;
}

export interface ActivityFeedItemDto {
  feedId: number;
  targetType: string;
  targetId: number;
  actionType: ActionType;
  actor: AuthorDto;
  content: ActivityFeedContent;
  createdAt: string;
  weight?: number;
}

export interface NewFeedCountResponseDto {
  newCount: number;
}
