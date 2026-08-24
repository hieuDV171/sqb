package com.frozenheart.backend.modules.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSemesterRequest(
        @NotBlank(message = "Tên học kỳ không được để trống")
        @Size(max = 100, message = "Tên học kỳ không được vượt quá 100 ký tự")
        String name
) {}
