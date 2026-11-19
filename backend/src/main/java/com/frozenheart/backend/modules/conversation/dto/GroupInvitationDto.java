package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Lời mời tham gia nhóm còn hiệu lực của người dùng hiện tại")
public record GroupInvitationDto(
        @Schema(description = "ID lời mời tham gia nhóm; dùng để chấp nhận hoặc từ chối", example = "42")
        Long invitationId,
        @Schema(description = "Tên nhóm trò chuyện", example = "Nhóm ôn tập Cơ sở dữ liệu")
        String groupName,
        @Schema(description = "URL ảnh đại diện nhóm, có thể null nếu nhóm chưa có ảnh", example = "https://media.example.com/group.png")
        String groupAvatarUrl,
        @Schema(description = "Tên người đã gửi lời mời nhóm", example = "Nguyễn Minh Anh")
        String inviterName,
        @Schema(description = "Thời điểm quyền/lời mời hết hiệu lực, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
        Instant expiresAt) {
}
