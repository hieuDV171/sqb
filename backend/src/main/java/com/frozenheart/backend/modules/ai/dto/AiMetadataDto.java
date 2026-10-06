package com.frozenheart.backend.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Thông tin kỹ thuật phản hồi từ mô hình AI (Model metadata)")
@Builder
public record AiMetadataDto(
        @Schema(description = "Tên mô hình AI được sử dụng", example = "qwen2.5-coder:7b")
        String model,

        @Schema(description = "Số lượng tokens đã tiêu thụ", example = "420")
        int tokensUsed,

        @Schema(description = "Thời gian xử lý của AI (ms)", example = "1250")
        long processingTimeMs
) {}