package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberDto {

    private Long userId;

    private String fullName;

    private String avatarUrl;

    private String frameUrl;

    private ConversationRole role;

    private Instant createdAt;
}
