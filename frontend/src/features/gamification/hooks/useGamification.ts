import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gamificationService } from '../services/gamificationService';
import type {
  LeaderboardPeriod,
  CosmeticType,
  CosmeticRarity,
  CosmeticSortBy,
  Game1PredictionRequest,
  Game2PredictionRequest,
  Game4PredictionRequest,
  Game6SubmitRequest,
  EquipCosmeticRequestDto,
  UnequipCosmeticRequestDto,
  ReviewErrorRequest,
} from '../types/gamification.types';
import { toast } from '@/stores/useToastStore';
import { useGamificationStore } from '../stores/useGamificationStore';

export const GAMIFICATION_KEYS = {
  all: ['gamification'] as const,
  leaderboard: (period?: LeaderboardPeriod, subjectId?: number) =>
    [...GAMIFICATION_KEYS.all, 'leaderboard', period, subjectId] as const,
  allBadges: () => [...GAMIFICATION_KEYS.all, 'badges'] as const,
  myBadges: () => [...GAMIFICATION_KEYS.all, 'badges', 'me'] as const,
  userBadges: (userId: number) => [...GAMIFICATION_KEYS.all, 'badges', 'user', userId] as const,
  shop: (type?: CosmeticType, sortBy?: CosmeticSortBy) =>
    [...GAMIFICATION_KEYS.all, 'shop', type, sortBy] as const,
  inventory: (type?: CosmeticType, rarity?: CosmeticRarity) =>
    [...GAMIFICATION_KEYS.all, 'inventory', type, rarity] as const,
  game1Classes: () => [...GAMIFICATION_KEYS.all, 'game1', 'classes'] as const,
  activeGame6: () => [...GAMIFICATION_KEYS.all, 'game6', 'active'] as const,
  myPredictions: () => [...GAMIFICATION_KEYS.all, 'my-predictions'] as const,
};

// ==========================================
// 1. LEADERBOARD & CHECK-IN
// ==========================================

export function useLeaderboard(params?: {
  period?: LeaderboardPeriod;
  subjectId?: number;
  after?: number;
  before?: number;
  limit?: number;
}) {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.leaderboard(params?.period, params?.subjectId),
    queryFn: async () => {
      const res = await gamificationService.getLeaderboard(params);
      return res.data;
    },
    enabled: params?.period !== 'SUBJECT' || (typeof params?.subjectId === 'number' && params.subjectId > 0),
    staleTime: 60 * 1000,
  });
}

export function useDailyCheckIn() {
  const queryClient = useQueryClient();
  const { setCheckInResult } = useGamificationStore();

  return useMutation({
    mutationFn: () => gamificationService.checkInDaily(),
    onSuccess: (res) => {
      if (res.data) {
        setCheckInResult(res.data.currentStreak, res.data.checkInDate);
      }
      toast.success(
        res.data?.message ||
          `Điểm danh thành công! +${res.data?.coinEarned} xu. Streak: ${res.data?.currentStreak} ngày.`
      );
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.leaderboard() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.myBadges() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.shop() });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Điểm danh thất bại. Vui lòng thử lại!';
      toast.error(msg);
    },
  });
}

// ==========================================
// 2. BADGES
// ==========================================

export function useAllBadges() {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.allBadges(),
    queryFn: async () => {
      const res = await gamificationService.getAllBadges();
      return res.data || [];
    },
    staleTime: 5 * 60 * 1000,
  });
}

export function useMyBadges() {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.myBadges(),
    queryFn: async () => {
      const res = await gamificationService.getMyBadges();
      return res.data || [];
    },
    staleTime: 60 * 1000,
  });
}

export function useUserBadges(userId: number) {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.userBadges(userId),
    queryFn: async () => {
      const res = await gamificationService.getUserBadges(userId);
      return res.data || [];
    },
    enabled: !!userId,
    staleTime: 60 * 1000,
  });
}

// ==========================================
// 3. SHOP & INVENTORY
// ==========================================

export function useShop(params?: {
  type?: CosmeticType;
  sortBy?: CosmeticSortBy;
  after?: number;
  limit?: number;
}) {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.shop(params?.type, params?.sortBy),
    queryFn: async () => {
      const res = await gamificationService.getShop(params);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useMyInventory(params?: {
  type?: CosmeticType;
  rarity?: CosmeticRarity;
  onlyUnlocked?: boolean;
  after?: number;
  limit?: number;
}) {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.inventory(params?.type, params?.rarity),
    queryFn: async () => {
      const res = await gamificationService.getMyInventory(params);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useBuyCosmetic() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (cosmeticId: number) => gamificationService.buyCosmetic(cosmeticId),
    onSuccess: (res) => {
      toast.success(res.message || 'Mua vật phẩm thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.shop() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.inventory() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể mua vật phẩm. Vui lòng kiểm tra số dư xu!');
    },
  });
}

export function useEquipCosmetic() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: EquipCosmeticRequestDto) => gamificationService.equipCosmetic(data),
    onSuccess: (res) => {
      toast.success(res.message || 'Trang bị thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.inventory() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.leaderboard() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể trang bị vật phẩm này.');
    },
  });
}

export function useUnequipCosmetic() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UnequipCosmeticRequestDto) => gamificationService.unequipCosmetic(data),
    onSuccess: (res) => {
      toast.success(res.message || 'Đã tháo trang bị!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.inventory() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.leaderboard() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể gỡ trang bị này.');
    },
  });
}

// ==========================================
// 4. PREDICTIONS
// ==========================================

export function useGame1Classes() {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.game1Classes(),
    queryFn: async () => {
      const res = await gamificationService.getMyCourseClassesForGame1();
      return res.data || [];
    },
  });
}

export function usePredictGame1() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Game1PredictionRequest) => gamificationService.predictGame1(data),
    onSuccess: () => {
      toast.success('Đặt dự đoán Game 1 thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.game1Classes() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.myPredictions() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể đặt dự đoán cho lớp này.');
    },
  });
}

export function usePredictGame2() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Game2PredictionRequest) => gamificationService.predictGame2(data),
    onSuccess: () => {
      toast.success('Đặt dự đoán Game 2 thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.myPredictions() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể đặt dự đoán Game 2.');
    },
  });
}

export function usePredictGame4() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Game4PredictionRequest) => gamificationService.predictGame4(data),
    onSuccess: () => {
      toast.success('Đặt dự đoán Game 4 thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.myPredictions() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể đặt dự đoán Game 4.');
    },
  });
}

export function useActiveGame6Session() {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.activeGame6(),
    queryFn: async () => {
      const res = await gamificationService.getActiveGame6Session();
      return res.data;
    },
    retry: false,
  });
}

export function useSubmitGame6() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Game6SubmitRequest) => gamificationService.submitGame6(data),
    onSuccess: () => {
      toast.success('Nộp thử thách Game 6 thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.activeGame6() });
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.myPredictions() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Không thể nộp dự đoán Game 6.');
    },
  });
}

export function useMyPredictions(params?: { after?: number; limit?: number }) {
  return useQuery({
    queryKey: GAMIFICATION_KEYS.myPredictions(),
    queryFn: async () => {
      const res = await gamificationService.getMyPredictions(params);
      return res.data;
    },
  });
}

export function useReviewErrorHunter() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      questionId,
      ratingUserId,
      data,
    }: {
      questionId: number;
      ratingUserId: number;
      data: ReviewErrorRequest;
    }) => gamificationService.reviewErrorGame5(questionId, ratingUserId, data),
    onSuccess: () => {
      toast.success('Đã thẩm định báo lỗi thành công!');
      queryClient.invalidateQueries({ queryKey: GAMIFICATION_KEYS.leaderboard() });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Lỗi khi thẩm định báo lỗi.');
    },
  });
}
