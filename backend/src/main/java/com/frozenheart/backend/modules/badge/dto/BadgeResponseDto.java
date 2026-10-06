package com.frozenheart.backend.modules.badge.dto;

import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.BadgeTier;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Schema(description = "Thông tin chi tiết huy hiệu danh hiệu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeResponseDto {

    @Schema(description = "ID huy hiệu", example = "1")
    private Long badgeId;

    @Schema(description = "Tên huy hiệu", example = "Ong Chăm Chỉ")
    private String name;

    @Schema(description = "Mô tả ý nghĩa của huy hiệu", example = "Duy trì chuỗi điểm danh 7 ngày liên tiếp")
    private String description;

    @Schema(description = "Cấp bậc huy hiệu", example = "BRONZE")
    private BadgeTier badgeTier;

    @Schema(description = "Sự kiện kích hoạt trao huy hiệu", example = "STUDY_STREAK")
    private BadgeTriggerEvent badgeTriggerEvent;

    @Schema(description = "URL hình ảnh biểu tượng huy hiệu", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/badges/streak_7.png")
    private String iconUrl;

    @Schema(description = "Tiêu chí đạt huy hiệu")
    private BadgeCriteria criteria;

    @Schema(description = "Tổng số người dùng đã đạt huy hiệu này", example = "156")
    private Integer totalEarnedUsers;

    @Schema(description = "Huy hiệu còn đang hoạt động hay không", example = "true")
    private Boolean active;

    @Schema(description = "Thời gian tạo huy hiệu", example = "2026-10-01T00:00:00Z")
    private Instant createdAt;
}

