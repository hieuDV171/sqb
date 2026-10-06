package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu tạo học kỳ mới trong hệ thống")
public record CreateSemesterRequest(
        @Schema(description = "Tên học kỳ hiển thị", example = "20241", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Tên học kỳ không được để trống")
        @Size(max = 100, message = "Tên học kỳ không được vượt quá 100 ký tự")
        String name
) {}
