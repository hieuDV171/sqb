package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu tạo môn học mới trong hệ thống")
public record CreateSubjectRequest(
        @Schema(description = "Tên môn học", example = "Nhập môn Công nghệ phần mềm", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Tên môn học không được để trống")
        @Size(max = 100, message = "Tên môn học tối đa 100 ký tự")
        String name,

        @Schema(description = "Mã môn học (duy nhất, viết hoa)", example = "IT3180", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Mã môn học không được để trống")
        @Size(max = 10, message = "Mã môn học tối đa 10 ký tự")
        String code
) {}
