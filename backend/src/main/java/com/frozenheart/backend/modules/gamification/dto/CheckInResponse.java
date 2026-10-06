package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Schema(description = "Kết quả điểm danh chuyên cần hàng ngày")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {

    @Schema(description = "Số xu nhận được từ lượt điểm danh này", example = "1.0")
    private double coinEarned;

    @Schema(description = "Tổng số dư xu mới sau điểm danh", example = "25.0")
    private double currentCoinBalance;

    @Schema(description = "Số ngày điểm danh liên tiếp hiện tại (Streak)", example = "7")
    private int currentStreak;

    @Schema(description = "Ngày điểm danh", example = "2026-10-06")
    private LocalDate checkInDate;

    @Schema(description = "Thông báo kết quả điểm danh", example = "Điểm danh thành công! Bạn nhận được +1.0 coin. Chuỗi hiện tại: 7 ngày.")
    private String message;

}

