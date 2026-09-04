package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.ConversationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private Long conversationId;

    private ConversationType type;

    private String displayName;

    private String displayAvatarUrl;

    private int memberCount;

    private LastMessageDto lastMessage;

    private int unreadCount;

    private boolean isMuted;

    private LocalDateTime hiddenAt;

    private LocalDateTime updatedAt;
}
