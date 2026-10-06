package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả chỉnh sửa câu hỏi của giảng viên")
@Builder
public record EditQuestionResponse(
        @Schema(description = "ID câu hỏi", example = "105")
        Long questionId,

        @Schema(description = "Trạng thái câu hỏi sau khi chỉnh sửa", example = "APPROVED")
        String status,

        @Schema(description = "ID giảng viên thực hiện chỉnh sửa", example = "5")
        Long reviewedBy,

        @Schema(description = "Thời gian chỉnh sửa", example = "2026-10-06T10:00:00Z")
        Instant reviewedAt) {
}

