package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trạng thái hoạt động của người đối thoại trong một cuộc chat trực tiếp")
public record ChatPresenceDto(
        @Schema(description = "ID cuộc hội thoại", example = "42") long conversationId,
        @Schema(description = "Người đối thoại có phiên đang hoạt động", example = "true") boolean active,
        @Schema(description = "Thời điểm đo theo mili giây Unix", example = "1795665600000") long observedAt) {
}
