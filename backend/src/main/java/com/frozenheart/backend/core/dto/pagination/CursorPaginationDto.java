package com.frozenheart.backend.core.dto.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;

@Builder
@Schema(description = "Thông tin phân trang ID cursor; giá trị null nghĩa không có mốc theo chiều tương ứng")
public record CursorPaginationDto(
        @Schema(description = "ID mốc để lấy trang trước theo chiều phân trang của endpoint", example = "3")
        Long before,
        @Schema(description = "ID mốc để lấy trang tiếp theo; bỏ trống ở trang đầu, dùng đúng cursor API trả về", example = "3")
        Long after,
        @Schema(description = "Còn trang theo chiều before hay không", example = "false")
        boolean hasPrev,
        @Schema(description = "Còn trang theo chiều after hay không", example = "true")
        boolean hasNext
) {}
