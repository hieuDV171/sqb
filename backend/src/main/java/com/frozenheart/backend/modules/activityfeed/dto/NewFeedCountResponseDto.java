package com.frozenheart.backend.modules.activityfeed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Số sự kiện mới để frontend quyết định làm mới bảng tin")
public class NewFeedCountResponseDto {
    @Schema(description = "Số sự kiện mới kể từ mốc client đã cung cấp", example = "3")
    private int newCount;
}
