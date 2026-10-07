import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  LeaderboardResponse,
  LeaderboardPeriod,
  CheckInResponse,
  RebuildLeaderboardRequest,
  BadgeResponseDto,
  UserBadgeResponseDto,
  CreateBadgeRequestDto,
  UpdateBadgeRequestDto,
  ManualGrantBadgeRequestDto,
  CosmeticShopResponseDto,
  MyInventoryResponseDto,
  ShopItemDto,
  EquipCosmeticRequestDto,
  EquipCosmeticResponseDto,
  UnequipCosmeticRequestDto,
  UnequipCosmeticResponseDto,
  CosmeticType,
  CosmeticRarity,
  CosmeticSortBy,
  MyCourseClassPredictionDto,
  Game1PredictionRequest,
  Game2PredictionRequest,
  Game4PredictionRequest,
  Game6ActiveSessionResponse,
  Game6SubmitRequest,
  GamePredictionResponse,
  MyPredictionsResponse,
  ReviewErrorRequest,
} from '../types/gamification.types';

export const gamificationService = {
  // ==========================================
  // 1. LEADERBOARD & CHECK-IN
  // ==========================================

  /**
   * 23.10 Lấy bảng xếp hạng vinh danh sinh viên
   * GET /api/v1/games/leaderboard
   */
  getLeaderboard: async (params?: {
    period?: LeaderboardPeriod;
    subjectId?: number;
    after?: number;
    before?: number;
    limit?: number;
  }): Promise<GlobalResponse<LeaderboardResponse>> => {
    return (await axiosClient.get<GlobalResponse<LeaderboardResponse>>('/games/leaderboard', {
      params,
    })) as any;
  },

  /**
   * 23.11 Điểm danh chuyên cần hàng ngày
   * POST /api/v1/games/check-in
   */
  checkInDaily: async (): Promise<GlobalResponse<CheckInResponse>> => {
    return (await axiosClient.post<GlobalResponse<CheckInResponse>>('/games/check-in')) as any;
  },

  /**
   * 24.1 Tái tạo lại Bảng xếp hạng trong Redis (Admin)
   * POST /api/v1/admin/leaderboards/rebuild
   */
  rebuildLeaderboard: async (request: RebuildLeaderboardRequest): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>('/admin/leaderboards/rebuild', request)) as any;
  },

  /**
   * 23.9 Chốt sổ tổng kết học kỳ (Admin)
   * POST /api/v1/admin/semesters/finalize-semester
   */
  finalizeSemester: async (): Promise<GlobalResponse<string>> => {
    return (await axiosClient.post<GlobalResponse<string>>('/admin/semesters/finalize-semester')) as any;
  },

  // ==========================================
  // 2. BADGES & ACHIVEMENTS
  // ==========================================

  /**
   * 25.1 Lấy danh sách tất cả các huy hiệu đang hoạt động
   * GET /api/v1/badges
   */
  getAllBadges: async (): Promise<GlobalResponse<BadgeResponseDto[]>> => {
    return (await axiosClient.get<GlobalResponse<BadgeResponseDto[]>>('/badges')) as any;
  },

  /**
   * 25.2 Lấy danh sách huy hiệu của chính người dùng hiện tại
   * GET /api/v1/badges/me
   */
  getMyBadges: async (): Promise<GlobalResponse<UserBadgeResponseDto[]>> => {
    return (await axiosClient.get<GlobalResponse<UserBadgeResponseDto[]>>('/badges/me')) as any;
  },

  /**
   * 25.3 Lấy danh sách huy hiệu của người dùng khác
   * GET /api/v1/badges/users/{userId}
   */
  getUserBadges: async (userId: number): Promise<GlobalResponse<UserBadgeResponseDto[]>> => {
    return (await axiosClient.get<GlobalResponse<UserBadgeResponseDto[]>>(`/badges/users/${userId}`)) as any;
  },

  /**
   * 25.4 Xem chi tiết một huy hiệu
   * GET /api/v1/badges/{badgeId}
   */
  getBadgeById: async (badgeId: number): Promise<GlobalResponse<BadgeResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<BadgeResponseDto>>(`/badges/${badgeId}`)) as any;
  },

  /**
   * 26.1 Admin lấy danh sách toàn bộ huy hiệu
   * GET /api/v1/admin/badges
   */
  getAdminBadges: async (): Promise<GlobalResponse<BadgeResponseDto[]>> => {
    return (await axiosClient.get<GlobalResponse<BadgeResponseDto[]>>('/admin/badges')) as any;
  },

  /**
   * 26.2 Admin tạo huy hiệu mới
   * POST /api/v1/admin/badges
   */
  createBadge: async (data: CreateBadgeRequestDto): Promise<GlobalResponse<BadgeResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<BadgeResponseDto>>('/admin/badges', data)) as any;
  },

  /**
   * 26.3 Admin cập nhật huy hiệu
   * PUT /api/v1/admin/badges/{badgeId}
   */
  updateBadge: async (
    badgeId: number,
    data: UpdateBadgeRequestDto
  ): Promise<GlobalResponse<BadgeResponseDto>> => {
    return (await axiosClient.put<GlobalResponse<BadgeResponseDto>>(`/admin/badges/${badgeId}`, data)) as any;
  },

  /**
   * 26.4 Admin vô hiệu hóa huy hiệu
   * DELETE /api/v1/admin/badges/{badgeId}
   */
  deleteBadge: async (badgeId: number): Promise<GlobalResponse<void>> => {
    return (await axiosClient.delete<GlobalResponse<void>>(`/admin/badges/${badgeId}`)) as any;
  },

  /**
   * 26.5 Admin cấp phát huy hiệu thủ công
   * POST /api/v1/admin/badges/{badgeId}/grant
   */
  grantManualBadge: async (
    badgeId: number,
    data: ManualGrantBadgeRequestDto
  ): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(`/admin/badges/${badgeId}/grant`, data)) as any;
  },

  // ==========================================
  // 3. COSMETICS & SHOP
  // ==========================================

  /**
   * 27.1 Xem túi đồ và bộ sưu tập vật phẩm của tôi
   * GET /api/v1/cosmetics/my-inventory
   */
  getMyInventory: async (params?: {
    type?: CosmeticType;
    rarity?: CosmeticRarity;
    onlyUnlocked?: boolean;
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<MyInventoryResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<MyInventoryResponseDto>>('/cosmetics/my-inventory', {
      params,
    })) as any;
  },

  /**
   * 27.2 Xem danh sách vật phẩm trong cửa hàng (Shop)
   * GET /api/v1/cosmetics/shop
   */
  getShop: async (params?: {
    type?: CosmeticType;
    sortBy?: CosmeticSortBy;
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<CosmeticShopResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<CosmeticShopResponseDto>>('/cosmetics/shop', {
      params,
    })) as any;
  },

  /**
   * 27.3 Mua vật phẩm từ cửa hàng bằng SQB Coins
   * POST /api/v1/cosmetics/{id}/buy
   */
  buyCosmetic: async (cosmeticId: number): Promise<GlobalResponse<ShopItemDto>> => {
    return (await axiosClient.post<GlobalResponse<ShopItemDto>>(`/cosmetics/${cosmeticId}/buy`)) as any;
  },

  /**
   * 27.4 Trang bị vật phẩm trang trí
   * PUT /api/v1/cosmetics/equip
   */
  equipCosmetic: async (data: EquipCosmeticRequestDto): Promise<GlobalResponse<EquipCosmeticResponseDto>> => {
    return (await axiosClient.put<GlobalResponse<EquipCosmeticResponseDto>>('/cosmetics/equip', data)) as any;
  },

  /**
   * 27.5 Tháo gỡ vật phẩm trang trí đang dùng
   * PUT /api/v1/cosmetics/unequip
   */
  unequipCosmetic: async (data: UnequipCosmeticRequestDto): Promise<GlobalResponse<UnequipCosmeticResponseDto>> => {
    return (await axiosClient.put<GlobalResponse<UnequipCosmeticResponseDto>>('/cosmetics/unequip', data)) as any;
  },

  // ==========================================
  // 4. PREDICTIONS & MINIGAMES
  // ==========================================

  /**
   * 23.1 Lấy danh sách lớp học phần để dự đoán Game 1
   * GET /api/v1/games/prediction/game-1/classes
   */
  getMyCourseClassesForGame1: async (): Promise<GlobalResponse<MyCourseClassPredictionDto[]>> => {
    return (await axiosClient.get<GlobalResponse<MyCourseClassPredictionDto[]>>(
      '/games/prediction/game-1/classes'
    )) as any;
  },

  /**
   * 23.2 Tham gia dự đoán Game 1: Số sinh viên nộp bài ngày mai
   * POST /api/v1/games/prediction/participants
   */
  predictGame1: async (data: Game1PredictionRequest): Promise<GlobalResponse<GamePredictionResponse>> => {
    return (await axiosClient.post<GlobalResponse<GamePredictionResponse>>(
      '/games/prediction/participants',
      data
    )) as any;
  },

  /**
   * 23.3 Tham gia dự đoán Game 2: Số câu hỏi duyệt trong phiên
   * POST /api/v1/games/prediction/approved-questions
   */
  predictGame2: async (data: Game2PredictionRequest): Promise<GlobalResponse<GamePredictionResponse>> => {
    return (await axiosClient.post<GlobalResponse<GamePredictionResponse>>(
      '/games/prediction/approved-questions',
      data
    )) as any;
  },

  /**
   * 23.4 Tham gia dự đoán Game 4: Tổng quy mô ngân hàng câu hỏi cuối kỳ
   * POST /api/v1/games/prediction/bank-size
   */
  predictGame4: async (data: Game4PredictionRequest): Promise<GlobalResponse<GamePredictionResponse>> => {
    return (await axiosClient.post<GlobalResponse<GamePredictionResponse>>(
      '/games/prediction/bank-size',
      data
    )) as any;
  },

  /**
   * 23.5 Lấy phiên Game 6 đang mở (Thứ 7 hàng tuần)
   * GET /api/v1/games/prediction/active-session
   */
  getActiveGame6Session: async (): Promise<GlobalResponse<Game6ActiveSessionResponse>> => {
    return (await axiosClient.get<GlobalResponse<Game6ActiveSessionResponse>>(
      '/games/prediction/active-session'
    )) as any;
  },

  /**
   * 23.6 Gửi dự đoán Game 6: Phán đoán câu hỏi do AI tạo
   * POST /api/v1/games/prediction/submit
   */
  submitGame6: async (data: Game6SubmitRequest): Promise<GlobalResponse<GamePredictionResponse>> => {
    return (await axiosClient.post<GlobalResponse<GamePredictionResponse>>(
      '/games/prediction/submit',
      data
    )) as any;
  },

  /**
   * 23.7 Lấy lịch sử các lượt dự đoán của tôi
   * GET /api/v1/games/my-predictions
   */
  getMyPredictions: async (params?: {
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<MyPredictionsResponse>> => {
    return (await axiosClient.get<GlobalResponse<MyPredictionsResponse>>('/games/my-predictions', {
      params,
    })) as any;
  },

  /**
   * 23.8 Giảng viên thẩm định báo lỗi câu hỏi (Game 5 - Error Hunter)
   * POST /api/v1/questions/{questionId}/ratings/{ratingUserId}/review-error
   */
  reviewErrorGame5: async (
    questionId: number,
    ratingUserId: number,
    data: ReviewErrorRequest
  ): Promise<GlobalResponse<string>> => {
    return (await axiosClient.post<GlobalResponse<string>>(
      `/questions/${questionId}/ratings/${ratingUserId}/review-error`,
      data
    )) as any;
  },
};
