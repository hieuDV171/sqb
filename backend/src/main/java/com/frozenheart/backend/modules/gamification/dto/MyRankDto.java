package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Thứ hạng cá nhân của người dùng hiện tại")
@Builder
public record MyRankDto(
        @Schema(description = "Thứ hạng hiện tại (1-based, trả về 0 nếu chưa có điểm)", example = "3")
        int rank,

        @Schema(description = "Tổng điểm tích lũy", example = "1250.0")
        double totalPoints,

        @Schema(description = "Xếp hạng phần trăm toàn trường (ví dụ 2.5 nghĩa là Top 2.5%)", example = "2.5")
        double topPercent,

        @Schema(description = "Tổng số người tham gia xếp hạng", example = "1200")
        int totalParticipants,

        @Schema(description = "Họ và tên người dùng", example = "Nguyễn Văn B")
        String fullName,

        @Schema(description = "URL ảnh đại diện", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/avatars/user42.png")
        String avatarUrl,

        @Schema(description = "URL khung viền đại diện", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/frames/top1.png")
        String frameUrl
) {}

