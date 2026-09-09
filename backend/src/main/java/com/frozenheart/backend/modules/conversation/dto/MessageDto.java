package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.MessageType;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String avatarUrl;

    private String frameUrl;

    private String content;

    private MessageType messageType;

    private List<String> mediaUrls;

    private ReplyMessagePreviewDto replyToMessage;

    private Instant createdAt;

    private boolean isEdited;

    private boolean isRevoked;
}
