package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.conversation.MessageType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tin nhắn gần nhất được phép hiển thị trong bản tóm tắt hội thoại")
public class LastMessageDto {

    @Schema(description = "ID tin nhắn", example = "42")
    private Long messageId;

    @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
    private String content;

    @Schema(description = "Loại nội dung tin nhắn: TEXT, IMAGE, VIDEO hoặc GIF")
    private MessageType messageType;

    @Schema(description = "Tin nhắn đã bị thu hồi với mọi người hay chưa", example = "false")
    private boolean revoked;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;
}
