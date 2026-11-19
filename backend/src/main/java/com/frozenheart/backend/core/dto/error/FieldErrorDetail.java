package com.frozenheart.backend.core.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Một lỗi validation theo trường trong data của response lỗi")
public record FieldErrorDetail(
        @Schema(description = "Tên/path trường bị lỗi", example = "email") String field,
        @Schema(description = "Lý do trường không hợp lệ", example = "Email không đúng định dạng") String message) {}
