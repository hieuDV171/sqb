package com.frozenheart.backend.modules.ai.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Tóm tắt thông tin phiên hội thoại AI của người dùng")
@Builder
public record AiChatSessionSummaryResponse(
        @Schema(description = "ID bản ghi phiên chat trong CSDL", example = "42")
        Long chatSessionId,

        @Schema(description = "Mã định danh phiên chat (UUID/String)", example = "session_a1b2c3d4")
        String sessionId,

        @Schema(description = "Tiêu đề tóm tắt cuộc trò chuyện do hệ thống tạo", example = "Thảo luận về danh sách liên kết")
        String title,

        @Schema(description = "Thời gian bắt đầu phiên", example = "2026-10-06T08:00:00Z")
        Instant createdAt,

        @Schema(description = "Thời gian tương tác gần nhất", example = "2026-10-06T08:35:00Z")
        Instant lastActiveAt
) {}
