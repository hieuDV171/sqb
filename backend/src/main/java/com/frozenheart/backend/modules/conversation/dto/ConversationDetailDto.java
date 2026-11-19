package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.conversation.ConversationType;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết cuộc hội thoại và các thành viên hiện có")
public class ConversationDetailDto {

    @Schema(description = "ID cuộc hội thoại mà người gọi là thành viên hoặc được phép truy cập", example = "42")
    private Long conversationId;

    @Schema(description = "Loại đối tượng theo enum của schema")
    private ConversationType type;

    @Schema(description = "Tên hiển thị theo ngữ cảnh của schema", example = "Nhóm ôn tập Cơ sở dữ liệu")
    private String name;

    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "Thành viên hiện có của cuộc hội thoại")
    private List<ConversationMemberDto> members;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;

    @Schema(description = "Số tài khoản được gửi lời mời thay vì thêm trực tiếp", example = "3")
    private int invitedMemberCount;
}
