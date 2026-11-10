package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.Message;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyMessagePreviewDto {

    private static final int MAX_PREVIEW_LENGTH = 60;

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String contentPreview;

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
