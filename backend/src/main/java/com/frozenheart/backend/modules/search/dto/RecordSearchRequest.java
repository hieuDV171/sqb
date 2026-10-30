package com.frozenheart.backend.modules.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Từ khóa người dùng đã chủ động xác nhận tìm kiếm")
public record RecordSearchRequest(
        @Schema(description = "Từ khóa hoàn chỉnh cần lưu vào lịch sử và thống kê thịnh hành", example = "Nguyễn Tiến Thành")
        @NotBlank(message = "Từ khóa tìm kiếm không được để trống")
        @Size(min = 2, max = 200, message = "Từ khóa tìm kiếm phải có từ 2 đến 200 ký tự")
        String query) {
}
