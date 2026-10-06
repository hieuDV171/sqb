package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

import com.frozenheart.backend.core.entity.session.SessionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Kết quả sau khi nộp đề xuất phiên câu hỏi")
public record ProposeSessionResponse(
        @Schema(description = "ID phiên đề xuất", example = "101")
        Long sessionId,

        @Schema(description = "Mã phiên nộp duy nhất", example = "IT3180_A7B2C1_1728200000000_101")
        String sessionCode,

        @Schema(description = "ID môn học", example = "10")
        Long subjectId,

        @Schema(description = "Số lượng câu hỏi trong phiên", example = "5")
        int questionCount,

        @Schema(description = "Trạng thái phiên đề xuất (PENDING, APPROVED, REJECTED, RESOLVED)", example = "PENDING")
        SessionStatus status,

        @Schema(description = "Thời điểm khởi tạo phiên")
        Instant createdAt
    ) {

}
