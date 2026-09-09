package com.frozenheart.backend.core.entity.user;

public enum CoinTransactionType {
    DAILY_CHECK_IN,       // Thưởng điểm danh chuyên cần
    BUY_COSMETIC,         // Mua vật phẩm trang trí (khung avatar, bóng chat...)
    QUESTION_APPROVED,    // Thưởng duyệt câu hỏi / phiên đề xuất
    GAME_REWARD,          // Thưởng minigame / giải đố / kết chuyển cuối kỳ
    LEADERBOARD_REWARD,   // Thưởng vinh danh bảng xếp hạng (đầu tháng / cuối kỳ)
    PENALTY_DEDUCTION,    // Phạt trừ xu do vi phạm hoặc câu hỏi lỗi
    ADMIN_ADJUSTMENT      // Điều chỉnh từ Quản trị viên
}
