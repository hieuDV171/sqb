package com.frozenheart.backend.modules.session.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AnswerQuestionRequest(
        @NotEmpty(message = "Danh sách đáp án chọn không được để trống")
        List<String> selectedOptions,

        @NotNull(message = "Thời gian làm bài không được để trống")
        Integer timeSpentSeconds,

        LocalDateTime questionUpdatedAt
) {}

