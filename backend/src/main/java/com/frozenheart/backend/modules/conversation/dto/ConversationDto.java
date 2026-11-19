package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.conversation.ConversationType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tóm tắt hội thoại cho danh sách chat của người dùng hiện tại")
public class ConversationDto {

    @Schema(description = "ID cuộc hội thoại mà người gọi là thành viên hoặc được phép truy cập", example = "42")
    private Long conversationId;

    @Schema(description = "Loại đối tượng theo enum của schema")
    private ConversationType type;

    @Schema(description = "Tên hội thoại hiển thị cho người gọi", example = "Nhóm ôn tập Cơ sở dữ liệu")
    private String displayName;

    @Schema(description = "URL ảnh đại diện hiển thị cho hội thoại theo góc nhìn của người gọi", example = "https://media.example.com/group.png")
    private String displayAvatarUrl;

    @Schema(description = "Số thành viên của cuộc hội thoại", example = "3")
    private int memberCount;

    @Schema(description = "Tin nhắn gần nhất được phép hiển thị, null khi chưa có tin nhắn phù hợp")
    private LastMessageDto lastMessage;

    @Schema(description = "Số mục chưa đọc của người dùng hiện tại theo ngữ cảnh API", example = "3")
    private int unreadCount;

    @Schema(description = "Người dùng hiện tại đã tắt thông báo của hội thoại hay chưa", example = "true")
    private boolean isMuted;

    @Schema(description = "Thời điểm ẩn hoặc mốc lịch sử bị ẩn theo ngữ cảnh hội thoại; có thể null", example = "2026-11-19T07:00:00Z")
    private Instant hiddenAt;

    @Schema(description = "Thời điểm cập nhật gần nhất, ISO-8601 UTC; có thể null nếu chưa ghi nhận", example = "2026-11-19T07:00:00Z")
    private Instant updatedAt;
}
