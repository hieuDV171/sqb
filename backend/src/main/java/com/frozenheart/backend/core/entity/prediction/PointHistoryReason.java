package com.frozenheart.backend.core.entity.prediction;

public enum PointHistoryReason {
    // Thưởng duyệt câu hỏi / phiên đề xuất
    APPROVED_QUESTION_REWARD,        // Thưởng khi câu hỏi đề xuất được GV duyệt
    SESSION_RESOLVED_BONUS,          // Thưởng hoàn tất phiên đề xuất câu hỏi
    AUTOMATED_SYSTEM_RESOLVE_BONUS,  // Hệ thống AI/Cron tự động duyệt phiên
    SESSION_RESOLVED_AUTHOR_AWARD,   // Thưởng tác giả khi phiên hoàn tất
    SESSION_RESOLVED_REVIEWER_AWARD, // Thưởng người duyệt khi phiên hoàn tất
    COURSE_CLASS_SESSION_AWARD,      // Thưởng hoàn tất phiên lớp học phần

    // Thưởng / phạt từ các Minigame
    GAME_1_WIN,                      // Thưởng thắng Game 1: Dự đoán sĩ số tham gia lớp học phần
    GAME_2_WIN,                      // Thưởng thắng Game 2: Dự đoán tỉ lệ duyệt câu hỏi LLM vs Human
    GAME_3_AUTHOR_CONFIDENCE_WIN,    // Thưởng thắng Game 3: Tác giả tự tin về độ chính xác câu hỏi
    GAME_4_TOP3_WINNER,              // Thưởng Top 3 dự đoán quy mô ngân hàng đề (Game 4)
    GAME_5_REPORT_ERROR_APPROVED,    // Thưởng báo lỗi câu hỏi chính xác (Điểm bí mật - Game 5)
    GAME_5_QUESTION_ERROR_PENALTY,   // Phạt tác giả câu hỏi có sai sót (Trừ điểm - Game 5)
    GAME_6_WIN,                      // Thưởng thắng trận đấu đối kháng (Game 6)
    GAME_6_PROPOSER_REWARD,          // Thưởng người khởi tạo trận thi đấu đối kháng (Game 6)

    // Điểm mặc định / điều chỉnh
    PUBLIC_POINTS_AWARDED,           // Cộng điểm công khai mặc định
    SECRET_POINTS_AWARDED,           // Cộng điểm bí mật mặc định
    PUBLIC_POINTS_DEDUCTED           // Trừ điểm công khai mặc định
}
