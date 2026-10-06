package com.frozenheart.backend.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "Yêu cầu AI tinh chỉnh câu hỏi trắc nghiệm")
@Builder
public record AiRefineRequest(
        @Schema(description = "Chỉ thị tinh chỉnh của người dùng (ví dụ: làm rõ câu hỏi, viết lại giải thích, sửa phương án gây nhiễu)", example = "Hãy viết lại đề bài ngắn gọn và làm phương án gây nhiễu hợp lý hơn")
        @NotBlank(message = "Prompt không được để trống")
        @Size(max = 1000, message = "Prompt không được vượt quá 1000 ký tự")
        String prompt,

        @Schema(description = "ID phiên chat AI liên quan (nếu có)", example = "session_xyz123")
        String chatSessionId
) {}
