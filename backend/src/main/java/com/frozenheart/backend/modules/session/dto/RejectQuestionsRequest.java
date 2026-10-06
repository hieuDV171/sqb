package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Schema(description = "Yêu cầu từ chối phê duyệt danh sách câu hỏi đề xuất")
@Builder
public record RejectQuestionsRequest(
        @Schema(description = "Danh sách ID câu hỏi bị từ chối", example = "[104, 105]")
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        List<Long> questionIds,

        @Schema(description = "Lý do từ chối phê duyệt", example = "Câu hỏi bị trùng lặp hoặc đáp án chưa chính xác")
        String reason
) {}

