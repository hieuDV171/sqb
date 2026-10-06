package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin thành viên trong cuộc hội thoại / nhóm chat")
public class ConversationMemberDto {

    @Schema(description = "ID người dùng", example = "5")
    private Long userId;

    @Schema(description = "Họ và tên hiển thị của thành viên", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "Đường dẫn ảnh đại diện", example = "https://minio.sqb.edu.vn/avatars/user5.jpg")
    private String avatarUrl;

    @Schema(description = "Đường dẫn khung viền avatar trang trí", example = "https://minio.sqb.edu.vn/frames/gold.png")
    private String frameUrl;

    @Schema(description = "Vai trò trong nhóm chat (CHIEF, VILLAGE_ELDER, VILLAGER)", example = "VILLAGER")
    private ConversationRole role;

    @Schema(description = "Thời điểm tham gia nhóm chat")
    private Instant createdAt;
}
