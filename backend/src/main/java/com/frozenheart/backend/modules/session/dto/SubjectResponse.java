package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Thông tin môn học trong hệ thống")
public record SubjectResponse(
        @Schema(description = "ID môn học", example = "10")
        Long subjectId,

        @Schema(description = "Mã môn học", example = "IT3180")
        String code,

        @Schema(description = "Tên môn học", example = "Nhập môn Công nghệ phần mềm")
        String name
) {
}
