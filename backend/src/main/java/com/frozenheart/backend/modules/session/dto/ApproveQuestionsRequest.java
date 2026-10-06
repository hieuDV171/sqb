package com.frozenheart.backend.modules.session.dto;

import java.util.List;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Schema(description = "Yêu cầu phê duyệt danh sách câu hỏi đề xuất")
@Builder
public record ApproveQuestionsRequest(
        @Schema(description = "Danh sách ID câu hỏi cần phê duyệt", example = "[101, 102, 103]")
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        List<Long> questionIds
) {}

