package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.MessageType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastMessageDto {

    private Long messageId;

    private String content;

    private MessageType messageType;

    private Instant createdAt;
}
