package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nội dung báo lỗi câu hỏi")
public record ReportQuestionRequest(
        @Schema(description = "Mô tả lỗi trong câu hỏi", example = "Đề bài thiếu dữ kiện để tính kết quả.",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
        @NotBlank @Size(max = 500) String description) {
}
