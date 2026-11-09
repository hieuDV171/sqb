package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả phê duyệt câu hỏi")
@Builder
public record ApproveQuestionsResponse(
        @Schema(description = "Điểm được cộng bởi thao tác này. Luôn bằng 0 vì điểm được ghi nhận khi chốt phiên", example = "0.0")
        double pointsEarned
) {}

