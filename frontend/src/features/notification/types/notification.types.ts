import type { CursorPagination } from '@/types/response.types';

// ==========================================
// 1. NOTIFICATION TYPES & ENUMS
// ==========================================

export type NotificationType =
  | 'FRIEND_REQUEST'
  | 'NEW_COMMENT'
  | 'QUESTION_APPROVED'
  | 'EARNED_BADGE'
  | 'QUESTION_ERROR_PENALTY'
  | 'GAME5_SEMESTER_RECAP'
  | 'LEADERBOARD_HONOR';

export type NotificationTargetType =
  | 'BADGE'
  | 'SESSION'
  | 'POST'
  | 'QUESTION'
  | 'USER'
  | 'CONVERSATION';

export interface NotificationTargetDto {
  type: NotificationTargetType;
  targetId?: number;
  url?: string;
}

export interface NotificationActorDto {
  id: number;
  username: string;
  fullName?: string;
  avatarUrl?: string;
}

export interface NotificationDto {
  notificationId: number;
  type: NotificationType;
  title: string;
  body: string;
  iconUrl?: string;
  readAt?: string;
  createdAt: string;
  target?: NotificationTargetDto;
  actor?: NotificationActorDto;
  metadata?: Record<string, any>;
}

export interface NotificationListResponseDto {
  items: NotificationDto[];
  pagination?: CursorPagination;
  unreadCount: number;
}

export interface ReadNotificationResponseDto {
  notificationId: number;
  readAt: string;
  unreadCount: number;
}

// ==========================================
// 2. PUSH SETTINGS & QUIET HOURS
// ==========================================

export type PushNotificationType =
  | 'SOCIAL'
  | 'ACADEMIC'
  | 'GAMIFICATION'
  | 'SYSTEM';

export interface PushCategorySettingDto {
  enabled: boolean;
  emailEnabled?: boolean;
}

export interface QuietHoursDto {
  enabled: boolean;
  startHour: number; // 0-23
  startMinute: number; // 0-59
  endHour: number; // 0-23
  endMinute: number; // 0-59
}

export interface PushSettingsResponseDto {
  categories: Record<PushNotificationType, PushCategorySettingDto>;
  quietHours: QuietHoursDto;
}

export interface UpdatePushSettingsRequestDto {
  categories?: Record<PushNotificationType, PushCategorySettingDto>;
  quietHours?: QuietHoursDto;
}

// ==========================================
// 3. ACTIVITY FEED (MODULE 14)
// ==========================================

export interface ActivityFeedItemDto {
  activityId: number;
  actorId: number;
  actorName: string;
  actorAvatarUrl?: string;
  activityType: string;
  summary: string;
  targetUrl?: string;
  createdAt: string;
}

export interface NewFeedCountResponseDto {
  newCount: number;
}
