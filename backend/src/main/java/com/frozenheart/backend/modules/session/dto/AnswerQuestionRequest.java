package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Schema(description = "Yêu cầu trả lời câu hỏi trắc nghiệm")
@Builder
public record AnswerQuestionRequest(
        @Schema(description = "Danh sách các khóa đáp án được chọn (A, B, C...)", example = "[\"A\"]")
        @NotEmpty(message = "Danh sách đáp án chọn không được để trống")
        List<String> selectedOptions,

        @Schema(description = "Thời điểm cập nhật câu hỏi gần nhất được cache tại client (dùng chống stale data)", example = "2026-10-06T08:00:00Z")
        Instant questionUpdatedAt) {
}

