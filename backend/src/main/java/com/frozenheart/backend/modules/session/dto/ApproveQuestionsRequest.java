package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record ApproveQuestionsRequest(
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        List<Long> questionIds
) {}
