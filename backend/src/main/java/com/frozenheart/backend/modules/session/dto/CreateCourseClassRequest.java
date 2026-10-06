package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu tạo lớp học phần mới")
public record CreateCourseClassRequest(
        @Schema(description = "Mã lớp học phần (VD: 171146)", example = "171146", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Mã lớp không được để trống")
        @Size(max = 50, message = "Mã lớp tối đa 50 ký tự")
        String classCode,

        @Schema(description = "ID môn học", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "subjectId không được để trống")
        Long subjectId,

        @Schema(description = "ID học kỳ (để trống sẽ tự động lấy học kỳ đang kích hoạt)", example = "1")
        Long semesterId,

        @Schema(description = "ID giảng viên phụ trách (chỉ Admin mới có quyền gán, để trống sẽ tự lấy ID người tạo)", example = "2")
        Long lecturerId
) {}
