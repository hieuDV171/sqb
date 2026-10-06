package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;

@Builder
@Schema(description = "Thông tin chi tiết của một học kỳ")
public record SemesterResponse(
        @Schema(description = "ID học kỳ", example = "1")
        Long semesterId,

        @Schema(description = "Tên học kỳ", example = "20241")
        String name,

        @Schema(description = "Trạng thái đang kích hoạt phục vụ học tập/thi", example = "true")
        boolean active,

        @Schema(description = "Trạng thái đã chốt sổ/tổng kết học kỳ (không thể kích hoạt lại)", example = "false")
        boolean isFinalize,

        @Schema(description = "Thời điểm khởi tạo học kỳ")
        Instant createdAt
    ) {
}
