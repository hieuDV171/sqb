package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "Phản hồi danh sách các lượt dự đoán minigame của người dùng")
@Builder
public record MyPredictionsResponse(
        @Schema(description = "Danh sách các lượt dự đoán")
        List<GamePredictionResponse> contents,

        @Schema(description = "Thông tin phân trang dạng con trỏ")
        CursorPaginationDto pagination
) {}

