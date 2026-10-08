package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Schema(description = "Trạng thái điểm danh chuyên cần của người dùng hôm nay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInStatusResponse {

    @Schema(description = "Số ngày điểm danh liên tiếp hiện tại (Streak)", example = "7")
    private int currentStreak;

    @Schema(description = "Đã điểm danh hôm nay chưa", example = "false")
    private boolean hasCheckedInToday;

    @Schema(description = "Ngày điểm danh gần nhất", example = "2026-10-07")
    private LocalDate lastCheckInDate;

    @Schema(description = "Ngày hôm nay theo múi giờ của người dùng", example = "2026-10-08")
    private LocalDate today;
}
