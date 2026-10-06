package com.frozenheart.backend.modules.ai.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Tin nhắn trong lịch sử trò chuyện AI")
@Builder
public record AiChatHistoryResponse(
        @Schema(description = "ID tin nhắn", example = "1001")
        Long messageId,

        @Schema(description = "ID phiên trò chuyện", example = "session_a1b2c3d4")
        String sessionId,

        @Schema(description = "Vai trò (user hoặc assistant)", example = "user")
        String role,

        @Schema(description = "Nội dung tin nhắn", example = "Giải thích giúp tôi về Binary Search Tree")
        String content,

        @Schema(description = "Thời điểm gửi tin nhắn", example = "2026-10-06T09:00:00Z")
        Instant createdAt
) {}
