package com.frozenheart.backend.core.dto.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import lombok.Builder;

@Builder
@Schema(description = "Một trang kết quả gồm items và pagination; trang trống trả danh sách rỗng")
public record CursorResponse<T>(
        @Schema(description = "Các mục thuộc trang hiện tại; danh sách rỗng khi không có kết quả")
        List<T> items,
        @Schema(description = "Mốc và cờ phân trang; không tự suy đoán cursor từ thứ tự hiển thị")
        CursorPaginationDto pagination
) {}
