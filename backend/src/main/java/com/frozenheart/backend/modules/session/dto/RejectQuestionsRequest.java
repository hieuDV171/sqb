package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import com.frozenheart.backend.core.entity.session.QuestionRejectionCode;

@Schema(description = "Yêu cầu từ chối phê duyệt danh sách câu hỏi đề xuất")
@Builder
public record RejectQuestionsRequest(
        @Schema(description = "Danh sách ID câu hỏi bị từ chối", example = "[104, 105]")
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        List<@NotNull(message = "ID câu hỏi không được để trống") Long> questionIds,

        @Schema(description = "Mã lý do từ chối chuẩn hóa", example = "DUPLICATE")
        QuestionRejectionCode rejectionCode,

        @Schema(description = "Lý do từ chối phê duyệt", example = "Câu hỏi bị trùng lặp hoặc đáp án chưa chính xác")
        @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự")
        String reason
) {}

