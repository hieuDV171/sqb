import type { CursorPagination } from '@/types/response.types';

// ==========================================
// 1. LEADERBOARD & CHECK-IN TYPES
// ==========================================

export type LeaderboardPeriod = 'SEMESTER' | 'SUBJECT';

export interface LeaderboardEntryDto {
  rank: number;
  userId: number;
  userCode: string;
  fullName: string;
  avatarUrl?: string;
  frameUrl?: string;
  totalPoints: number;
  isCurrentUser: boolean;
}

export interface MyRankDto {
  rank: number;
  totalPoints: number;
  topPercent: number;
  totalParticipants: number;
  fullName: string;
  avatarUrl?: string;
  frameUrl?: string;
}

export interface LeaderboardResponse {
  entries: LeaderboardEntryDto[];
  myRank: MyRankDto;
  pagination?: CursorPagination;
}

export interface CheckInResponse {
  coinEarned: number;
  currentCoinBalance: number;
  currentStreak: number;
  checkInDate: string;
  message: string;
}

export interface CheckInStatusResponse {
  currentStreak: number;
  hasCheckedInToday: boolean;
  lastCheckInDate?: string;
  today: string;
}

export interface RebuildLeaderboardRequest {
  type: string;
  subjectId?: number;
}

// ==========================================
// 2. BADGE TYPES
// ==========================================

export type BadgeTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND';

export type BadgeTriggerEvent =
  | 'APPROVED_QUESTIONS'
  | 'PROPOSED_QUESTIONS'
  | 'REVIEWED_SESSIONS'
  | 'ACCEPTED_FRIENDS'
  | 'PUBLIC_POINTS'
  | 'STUDY_STREAK'
  | 'LEADERBOARD_RANK'
  | 'MANUAL_GRANT';

export interface BadgeCriteria {
  type?: string;
  threshold?: number;
  [key: string]: unknown;
}

export interface BadgeResponseDto {
  badgeId: number;
  name: string;
  description: string;
  badgeTier: BadgeTier;
  badgeTriggerEvent?: BadgeTriggerEvent;
  iconUrl?: string;
  criteria?: BadgeCriteria;
  totalEarnedUsers?: number;
  active?: boolean;
  createdAt?: string;
}

export interface UserBadgeResponseDto {
  badgeId: number;
  name: string;
  description: string;
  badgeTier: BadgeTier;
  iconUrl?: string;
  earnedAt: string;
}

export interface CreateBadgeRequestDto {
  name: string;
  description: string;
  badgeTier: BadgeTier;
  badgeTriggerEvent: BadgeTriggerEvent;
  iconUrl: string;
  criteria: BadgeCriteria;
}

export interface UpdateBadgeRequestDto {
  name?: string;
  description?: string;
  badgeTier?: BadgeTier;
  iconUrl?: string;
  criteria?: BadgeCriteria;
  active?: boolean;
}

export interface ManualGrantBadgeRequestDto {
  userIds: number[];
  reason?: string;
}

// ==========================================
// 3. COSMETIC & SHOP TYPES
// ==========================================

export type CosmeticType = 'AVATAR_FRAME' | 'PROFILE_PIN' | 'CHAT_BUBBLE' | 'BADGE_EFFECT';

export type CosmeticRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';

export type CosmeticSortBy = 'NEWEST' | 'PRICE_ASC' | 'PRICE_DESC' | 'RARITY';

export interface CosmeticSummaryDto {
  totalOwned?: number;
  totalAvailable?: number;
  completionRate?: number;
}

export interface EquippedItemDto {
  cosmeticId: number;
  name: string;
  assetUrl?: string;
}

export interface InventoryItemDto {
  cosmeticId: number;
  name: string;
  description: string;
  type: CosmeticType;
  rarity: CosmeticRarity;
  assetUrl?: string;
  unlockedAt: string;
  acquireMethod?: string;
  availableUntil?: string;
}

export interface MyInventoryResponseDto {
  summary?: CosmeticSummaryDto;
  currentlyEquipped?: Record<string, EquippedItemDto>;
  inventory: InventoryItemDto[];
  pagination?: CursorPagination;
}

export interface ShopItemDto {
  cosmeticId: number;
  name: string;
  description: string;
  type: CosmeticType;
  rarity: CosmeticRarity;
  assetUrl?: string;
  price: number;
  originalPrice?: number;
  availableUntil?: string;
  isOwned: boolean;
}

export interface CosmeticShopResponseDto {
  userPoints: number;
  items: ShopItemDto[];
  pagination?: CursorPagination;
}

export interface EquipCosmeticRequestDto {
  cosmeticId: number;
}

export interface EquipCosmeticResponseDto {
  equippedCosmeticId: number;
  name: string;
  type: CosmeticType;
  assetUrl?: string;
  message?: string;
}

export interface UnequipCosmeticRequestDto {
  cosmeticId: number;
}

export interface UnequipCosmeticResponseDto {
  unequippedCosmeticId: number;
  message?: string;
}

// ==========================================
// 4. MINIGAMES & PREDICTION TYPES
// ==========================================

export type GameType =
  | 'GAME_1_PARTICIPANTS'
  | 'GAME_2_APPROVED_QUESTIONS'
  | 'GAME_4_BANK_SIZE'
  | 'GAME_5_ERROR_HUNTER'
  | 'GAME_6_AI_DETECTION';

export type PredictionStatus = 'PENDING' | 'RESOLVED' | 'CANCELLED';

export interface MyCourseClassPredictionDto {
  courseClassId: number;
  classCode: string;
  semester: string;
  subjectId: number;
  subjectName: string;
  lecturerName: string;
  alreadyPredicted: boolean;
  predictedCount?: number;
}

export interface Game1PredictionRequest {
  courseClassId: number;
  predictedCount: number;
}

export interface Game2PredictionRequest {
  sessionId: number;
  predictedLlmCount?: number;
  predictedHumanCount?: number;
}

export interface Game4PredictionRequest {
  subjectId: number;
  predictedBankSize: number;
}

export interface Game6QuestionOption {
  key: string;
  content: string;
}

export interface Game6QuestionDto {
  questionId: number;
  content: string;
  options: Game6QuestionOption[];
  explanation?: string;
  imageUrls?: string[];
}

export interface Game6ActiveSessionResponse {
  sessionId: number;
  weekNumber: number;
  year: number;
  startTime: string;
  endTime: string;
  questions: Game6QuestionDto[];
}

export interface Game6SubmitRequest {
  minigameSessionId: number;
  selectedLlmQuestionIds: number[];
}

export interface GamePredictionResponse {
  predictionId: number;
  gameType: GameType;
  targetType: string;
  targetId: number;
  predictionData: unknown;
  actualData?: unknown;
  status: PredictionStatus;
  isCorrect: boolean;
  createdAt: string;
  resolvedAt?: string;
}

export interface MyPredictionsResponse {
  contents: GamePredictionResponse[];
  pagination?: CursorPagination;
}

export interface ReviewErrorRequest {
  isComfirmed: boolean;
  notes?: string;
}

// ==========================================
// 5. HELPER UTILITIES
// ==========================================

export function getTierColor(tier: BadgeTier): {
  badgeBg: string;
  badgeBorder: string;
  textColor: string;
  linearGradient: string;
  shadowColor: string;
} {
  switch (tier) {
    case 'BRONZE':
      return {
        badgeBg: 'bg-amber-700/10 dark:bg-amber-900/20',
        badgeBorder: 'border-amber-700/30',
        textColor: 'text-amber-700 dark:text-amber-400',
        linearGradient: 'from-amber-600 to-amber-800',
        shadowColor: 'shadow-amber-700/20',
      };
    case 'SILVER':
      return {
        badgeBg: 'bg-slate-300/20 dark:bg-slate-700/30',
        badgeBorder: 'border-slate-400/40',
        textColor: 'text-slate-600 dark:text-slate-300',
        linearGradient: 'from-slate-400 to-slate-600',
        shadowColor: 'shadow-slate-500/20',
      };
    case 'GOLD':
      return {
        badgeBg: 'bg-amber-400/15 dark:bg-amber-500/15',
        badgeBorder: 'border-amber-400/40',
        textColor: 'text-amber-500 dark:text-amber-300',
        linearGradient: 'from-amber-400 to-yellow-500',
        shadowColor: 'shadow-amber-500/30',
      };
    case 'PLATINUM':
      return {
        badgeBg: 'bg-cyan-400/15 dark:bg-cyan-500/20',
        badgeBorder: 'border-cyan-400/40',
        textColor: 'text-cyan-600 dark:text-cyan-300',
        linearGradient: 'from-cyan-400 to-teal-500',
        shadowColor: 'shadow-cyan-500/30',
      };
    case 'DIAMOND':
      return {
        badgeBg: 'bg-indigo-500/15 dark:bg-indigo-500/25',
        badgeBorder: 'border-indigo-400/50',
        textColor: 'text-indigo-600 dark:text-indigo-300',
        linearGradient: 'from-indigo-500 via-purple-500 to-pink-500',
        shadowColor: 'shadow-indigo-500/40',
      };
    default:
      return {
        badgeBg: 'bg-slate-100 dark:bg-slate-800',
        badgeBorder: 'border-slate-300',
        textColor: 'text-slate-600 dark:text-slate-400',
        linearGradient: 'from-slate-400 to-slate-600',
        shadowColor: 'shadow-slate-400/20',
      };
  }
}

export function getRarityColor(rarity: CosmeticRarity): {
  badge: string;
  border: string;
  glow: string;
  text: string;
} {
  switch (rarity) {
    case 'COMMON':
      return {
        badge: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
        border: 'border-slate-200 dark:border-slate-700',
        glow: 'group-hover:border-slate-300 dark:group-hover:border-slate-600',
        text: 'text-slate-600 dark:text-slate-400',
      };
    case 'RARE':
      return {
        badge: 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/50 dark:text-blue-300 dark:border-blue-800',
        border: 'border-blue-200 dark:border-blue-800/60',
        glow: 'group-hover:border-blue-400 group-hover:shadow-md group-hover:shadow-blue-500/10',
        text: 'text-blue-600 dark:text-blue-400',
      };
    case 'EPIC':
      return {
        badge: 'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/50 dark:text-purple-300 dark:border-purple-800',
        border: 'border-purple-200 dark:border-purple-800/60',
        glow: 'group-hover:border-purple-400 group-hover:shadow-md group-hover:shadow-purple-500/10',
        text: 'text-purple-600 dark:text-purple-400',
      };
    case 'LEGENDARY':
      return {
        badge: 'bg-linear-to-r from-amber-500 to-yellow-500 text-white shadow-xs',
        border: 'border-amber-300 dark:border-amber-600/60',
        glow: 'group-hover:border-amber-400 group-hover:shadow-lg group-hover:shadow-amber-500/20',
        text: 'text-amber-500 dark:text-amber-400 font-bold',
      };
  }
}

export function calculateUserLevel(totalPoints: number): {
  level: number;
  currentPointsInLevel: number;
  pointsForNextLevel: number;
  progressPercent: number;
  levelTitle: string;
} {
  const points = Math.max(0, totalPoints);
  const POINTS_PER_LEVEL = 100;
  const level = Math.floor(points / POINTS_PER_LEVEL) + 1;
  const currentPointsInLevel = points % POINTS_PER_LEVEL;
  const pointsForNextLevel = POINTS_PER_LEVEL;
  const progressPercent = Math.min(100, Math.round((currentPointsInLevel / pointsForNextLevel) * 100));

  let levelTitle = 'Tập sự';
  if (level >= 50) levelTitle = 'Huyền thoại Bách Khoa';
  else if (level >= 30) levelTitle = 'Đại sư Tri thức';
  else if (level >= 20) levelTitle = 'Kiện tướng Học thuật';
  else if (level >= 10) levelTitle = 'Cao thủ Ôn thi';
  else if (level >= 5) levelTitle = 'Học giả Tiên phong';

  return {
    level,
    currentPointsInLevel,
    pointsForNextLevel,
    progressPercent,
    levelTitle,
  };
}
