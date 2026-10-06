package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Mục xếp hạng trên Bảng vinh danh")
@Builder
public record LeaderboardEntryDto(
        @Schema(description = "Thứ hạng (bắt đầu từ 1)", example = "1")
        int rank,

        @Schema(description = "ID người dùng", example = "42")
        Long userId,

        @Schema(description = "Mã người dùng / Mã sinh viên", example = "20210001")
        String userCode,

        @Schema(description = "Họ và tên người dùng", example = "Nguyễn Văn B")
        String fullName,

        @Schema(description = "URL ảnh đại diện", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/avatars/user42.png")
        String avatarUrl,

        @Schema(description = "URL khung viền đại diện", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/frames/top1.png")
        String frameUrl,

        @Schema(description = "Tổng điểm tích lũy", example = "1520.5")
        double totalPoints,

        @Schema(description = "Có phải chính người dùng đang truy cập hay không", example = "false")
        boolean isCurrentUser
) {}

