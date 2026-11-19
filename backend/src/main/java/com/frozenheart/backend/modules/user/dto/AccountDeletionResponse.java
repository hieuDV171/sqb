package com.frozenheart.backend.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Thời điểm dự kiến xóa tài khoản cùng khả năng hủy yêu cầu")
public record AccountDeletionResponse(
        @Schema(description = "Thời điểm dự kiến xóa tài khoản", example = "2026-11-19T07:00:00Z")
        Instant scheduledFor,
        @Schema(description = "Yêu cầu xóa tài khoản còn được phép hủy hay không", example = "true")
        boolean canCancel) {
}
