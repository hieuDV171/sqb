package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ứng viên thêm/mời nhóm từ quan hệ bạn bè hoặc theo dõi")
public record GroupCandidateDto(
        @Schema(description = "ID tài khoản trong SQB", example = "42")
        Long userId,
        @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
        String fullName,
        @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
        String avatarUrl,
        @Schema(description = "Tài khoản này là bạn bè của người gọi", example = "true")
        boolean friend,
        @Schema(description = "Người gọi đang theo dõi tài khoản này", example = "true")
        boolean following,
        @Schema(description = "Tài khoản này đang theo dõi người gọi", example = "true")
        boolean follower) {
}
