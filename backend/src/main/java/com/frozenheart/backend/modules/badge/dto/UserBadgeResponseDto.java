package com.frozenheart.backend.modules.badge.dto;

import com.frozenheart.backend.core.entity.badge.BadgeTier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Schema(description = "Thông tin huy hiệu mà người dùng đã sở hữu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadgeResponseDto {

    @Schema(description = "ID huy hiệu", example = "1")
    private Long badgeId;

    @Schema(description = "Tên huy hiệu", example = "Chiến Binh Điểm Danh")
    private String name;

    @Schema(description = "Mô tả huy hiệu", example = "Điểm danh liên tục trong 30 ngày")
    private String description;

    @Schema(description = "Cấp bậc huy hiệu", example = "SILVER")
    private BadgeTier badgeTier;

    @Schema(description = "URL hình ảnh huy hiệu", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/badges/checkin_silver.png")
    private String iconUrl;

    @Schema(description = "Thời điểm người dùng đạt được huy hiệu", example = "2026-10-05T12:00:00Z")
    private Instant earnedAt;
}
