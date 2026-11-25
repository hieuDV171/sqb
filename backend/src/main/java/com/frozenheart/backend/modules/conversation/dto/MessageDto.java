package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.conversation.MessageType;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tin nhắn hoặc dòng hệ thống trong lịch sử hội thoại")
public class MessageDto {

    @Schema(description = "ID tin nhắn", example = "42")
    private Long messageId;

    @Schema(description = "ID tài khoản gửi tin nhắn, null với dòng hệ thống", example = "42")
    private Long senderId;

    @Schema(description = "Tên hiển thị người gửi tin nhắn, null với dòng hệ thống", example = "Nguyễn Minh Anh")
    private String senderName;

    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "URL khung trang trí avatar, null/rỗng khi không có", example = "https://media.example.com/frame.png")
    private String frameUrl;

    @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
    private String content;

    @Schema(description = "Loại nội dung tin nhắn: TEXT, IMAGE, VIDEO, GIF hoặc SYSTEM")
    private MessageType messageType;

    @Schema(description = "Các tệp đính kèm; kiểu phần tử theo schema của endpoint", example = "[\"https://media.example.com/image.png\"]")
    private List<String> mediaUrls;

    @Schema(description = "Bản trích dẫn tin nhắn gốc khi trả lời; null với tin nhắn độc lập")
    private ReplyMessagePreviewDto replyToMessage;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;

    @Schema(description = "Nội dung tin nhắn đã từng được chỉnh sửa", example = "false")
    private boolean isEdited;

    @Schema(description = "Tin nhắn đã bị thu hồi với mọi người hay chưa", example = "false")
    private boolean revoked;
}
