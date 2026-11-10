package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record HiddenChatUnlockDto(
        @Schema(description = "Thời điểm quyền mở kho ẩn hết hạn") Instant expiresAt) {
}
