package com.frozenheart.backend.modules.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank(message = "Tên môn học không được để trống")
        @Size(max = 100, message = "Tên môn học tối đa 100 ký tự")
        String name,

        @NotBlank(message = "Mã môn học không được để trống")
        @Size(max = 10, message = "Mã môn học tối đa 10 ký tự")
        String code
) {}
