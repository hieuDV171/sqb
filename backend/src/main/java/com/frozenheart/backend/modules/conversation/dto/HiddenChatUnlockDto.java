package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Kết quả mở kho chat ẩn của phiên JWT hiện tại")
public record HiddenChatUnlockDto(
        @Schema(description = "Thời điểm quyền mở kho ẩn hết hạn", example = "2026-11-19T07:00:00Z") Instant expiresAt) {
}
