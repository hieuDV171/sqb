package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastMessageDto {

    private Long id;

    private String content;

    private MessageType messageType;

    private LocalDateTime createdAt;
}
