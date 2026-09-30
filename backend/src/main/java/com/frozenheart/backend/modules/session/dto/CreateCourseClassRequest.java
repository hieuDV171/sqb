package com.frozenheart.backend.modules.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCourseClassRequest(
        @NotBlank(message = "Mã lớp không được để trống")
        @Size(max = 50, message = "Mã lớp tối đa 50 ký tự")
        String classCode,

        @NotNull(message = "subjectId không được để trống")
        Long subjectId,

        Long semesterId,

        Long lecturerId
) {}
