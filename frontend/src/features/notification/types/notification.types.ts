import type { CursorPagination } from '@/types/response.types';

// ==========================================
// 1. NOTIFICATION TYPES & ENUMS
// ==========================================

export type NotificationType =
  | 'FRIEND_REQUEST'
  | 'NEW_COMMENT'
  | 'QUESTION_APPROVED'
  | 'EARNED_BADGE'
  | 'BADGE_REVOKED'
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
  metadata?: Record<string, unknown>;
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
  description?: string;
}

export interface QuietHoursDto {
  enabled: boolean;
  startTime: string; // HH:mm
  endTime: string; // HH:mm
}

export interface PushSettingsResponseDto {
  pushEnabled: boolean;
  categories: Record<PushNotificationType, PushCategorySettingDto>;
  quietHours: QuietHoursDto;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
}

export type UpdatePushSettingsRequestDto = PushSettingsResponseDto;
