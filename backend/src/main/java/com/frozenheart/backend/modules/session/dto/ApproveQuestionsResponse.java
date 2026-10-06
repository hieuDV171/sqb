package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả phê duyệt câu hỏi")
@Builder
public record ApproveQuestionsResponse(
        @Schema(description = "Tổng điểm thưởng đã cộng cho tác giả", example = "30.0")
        double pointsEarned
) {}

