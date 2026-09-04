package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.ConversationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailDto {

    private Long conversationId;

    private ConversationType type;

    private String name;

    private String avatarUrl;

    private List<ConversationMemberDto> members;

    private LocalDateTime createdAt;
}
