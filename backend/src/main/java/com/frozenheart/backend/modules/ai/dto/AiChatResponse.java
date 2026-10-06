package com.frozenheart.backend.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Câu trả lời từ trợ lý AI")
@Builder
public record AiChatResponse(
        @Schema(description = "Nội dung phản hồi được định dạng Markdown của AI", example = "ArrayList dựa trên mảng động nên truy xuất ngẫu nhiên O(1)...")
        String aiResponse,

        @Schema(description = "ID phiên trò chuyện", example = "session_a1b2c3d4")
        String sessionId,

        @Schema(description = "Thông số kỹ thuật tiêu thụ tài nguyên của mô hình")
        AiMetadataDto aiMetadata
) {}
