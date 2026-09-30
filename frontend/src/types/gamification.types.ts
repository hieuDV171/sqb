export type LeaderboardScope = 'SEMESTER' | 'SUBJECT';
export type BadgeTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';

/**
 * SubjectResponse.java (/public/subjects)
 */
export interface SubjectResponse {
  id: number;
  code: string;
  name: string;
}

/**
 * LeaderboardEntryDto.java
 */
export interface LeaderboardEntryDto {
  rank: number;
  userId: number;
  userCode: string; // MSSV (Mã số sinh viên, ví dụ: 20210001)
  fullName: string;
  avatarUrl?: string;
  frameUrl?: string;
  totalPoints: number;
  isCurrentUser: boolean;
}

/**
 * MyRankDto.java
 */
export interface MyRankDto {
  rank: number;
  totalPoints: number;
  topPercent: number; // Tỷ lệ phần trăm top, ví dụ: 4.5%
  totalParticipants: number; // Tổng số sinh viên tham gia kỳ thi
  fullName: string;
  avatarUrl?: string;
  frameUrl?: string;
}

/**
 * LeaderboardResponse.java
 */
export interface LeaderboardResponse {
  entries: LeaderboardEntryDto[];
  myRank: MyRankDto;
  pagination?: {
    after?: number;
    hasNext: boolean;
  };
}

export type BadgeTriggerEvent =
  | 'APPROVED_QUESTIONS'
  | 'PROPOSED_QUESTIONS'
  | 'REVIEWED_SESSIONS'
  | 'ACCEPTED_FRIENDS'
  | 'PUBLIC_POINTS'
  | 'STUDY_STREAK'
  | 'LEADERBOARD_RANK'
  | 'MANUAL_GRANT';

/**
 * BadgeResponseDto.java (Toàn bộ danh mục huy hiệu hệ thống)
 */
export interface BadgeResponseDto {
  id: number;
  name: string;
  description: string;
  badgeTier: BadgeTier;
  badgeTriggerEvent?: BadgeTriggerEvent;
  iconUrl?: string;
  criteria?: {
    type?: string;
    threshold?: number;
    [key: string]: unknown;
  };
  totalEarnedUsers?: number;
  active?: boolean;
  createdAt?: string;
}

/**
 * UserBadgeResponseDto.java (Huy hiệu người dùng cụ thể đã đạt được)
 */
export interface UserBadgeResponseDto {
  badgeId: number;
  name: string;
  description: string;
  badgeTier: BadgeTier;
  iconUrl?: string;
  earnedAt: string;
}

/**
 * Helper hiển thị nhãn tiếng Việt cho BadgeTriggerEvent
 */
export function getBadgeTriggerLabel(event?: BadgeTriggerEvent): string {
  switch (event) {
    case 'APPROVED_QUESTIONS':
      return 'Duyệt câu hỏi';
    case 'PROPOSED_QUESTIONS':
      return 'Đề xuất câu hỏi';
    case 'REVIEWED_SESSIONS':
      return 'Thẩm định phiên đề';
    case 'ACCEPTED_FRIENDS':
      return 'Kết nối bạn bè';
    case 'PUBLIC_POINTS':
      return 'Điểm cống hiến';
    case 'STUDY_STREAK':
      return 'Chuỗi ngày ôn tập';
    case 'LEADERBOARD_RANK':
      return 'Thứ hạng BXH';
    case 'MANUAL_GRANT':
      return 'Vinh danh đặc cách';
    default:
      return 'Thành tích học tập';
  }
}

/**
 * Helper tính số xu thưởng Client-side dựa trên Quy chế vinh danh cố định của trường
 * - SEMESTER: Thưởng cuối kỳ (Top 1: 300 xu, Top 2: 180 xu, Top 3: 90 xu; Hàng tháng: 30 / 20 / 10 xu)
 * - SUBJECT: Vinh danh học thuật (không phát xu tự động để tránh lạm phát ví)
 */
export function getRewardCoins(rank: number, scope: LeaderboardScope): number {
  if (scope === 'SEMESTER') {
    if (rank === 1) return 300;
    if (rank === 2) return 180;
    if (rank === 3) return 90;
    return 0;
  }
  // BXH theo Môn học: vinh danh danh hiệu chuyên môn, không thưởng xu
  return 0;
}

/**
 * Danh hiệu học thuật danh dự cho Top sinh viên theo Môn học (SUBJECT)
 */
export function getSubjectHonorTitle(rank: number): string {
  if (rank === 1) return 'Thủ Khoa Môn';
  if (rank === 2) return 'Á Khoa Môn';
  if (rank === 3) return 'Kiện Tướng Môn';
  return '';
}
