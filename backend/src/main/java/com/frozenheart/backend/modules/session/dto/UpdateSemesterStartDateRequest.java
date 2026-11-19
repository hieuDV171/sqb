package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Ngày bắt đầu học kỳ cần cập nhật bởi quản trị viên")
public record UpdateSemesterStartDateRequest(
        @Schema(description = "Ngày khai giảng mới", example = "2026-09-07", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Ngày khai giảng không được để trống")
        LocalDate startDate
) {}
