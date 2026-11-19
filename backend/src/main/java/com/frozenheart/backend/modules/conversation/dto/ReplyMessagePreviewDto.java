package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.conversation.Message;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bản trích dẫn tin nhắn gốc; unavailable=true khi tin gốc không còn được phép hiển thị")
public class ReplyMessagePreviewDto {

    private static final int MAX_PREVIEW_LENGTH = 60;

    @Schema(description = "ID tin nhắn", example = "42")
    private Long messageId;

    @Schema(description = "ID tài khoản gửi tin nhắn", example = "42")
    private Long senderId;

    @Schema(description = "Tên hiển thị người gửi tin nhắn", example = "Nguyễn Minh Anh")
    private String senderName;

    @Schema(description = "Nội dung trích dẫn rút gọn của tin nhắn gốc", example = "Nội dung tin nhắn được trả lời")
    private String contentPreview;

    @Schema(description = "Tin nhắn gốc đã bị xóa/ẩn/không còn được người gọi xem", example = "false")
    private boolean unavailable;

    public static ReplyMessagePreviewDto fromMessage(Message message, String senderName) {
        return builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderName(senderName)
                .contentPreview(previewContent(message))
                .build();
    }

    public static ReplyMessagePreviewDto unavailable(Long messageId) {
        return builder().messageId(messageId).unavailable(true)
                .contentPreview("Tin nhắn gốc không còn hiển thị").build();
    }

    private static String previewContent(Message message) {
        if (message.getDeletedAt() != null) {
            return "Tin nhắn đã được thu hồi";
        }
        String content = message.getContent();
        if (content != null && !content.isBlank()) {
            String text = content.strip();
            return text.codePointCount(0, text.length()) > MAX_PREVIEW_LENGTH
                    ? text.substring(0, text.offsetByCodePoints(0, MAX_PREVIEW_LENGTH)) + "…"
                    : text;
        }
        return switch (message.getMessageType()) {
            case IMAGE -> "[Hình ảnh]";
            case VIDEO -> "[Video]";
            case GIF -> "[Ảnh GIF]";
            case null, default -> "[Tệp đính kèm]";
        };
    }
}
