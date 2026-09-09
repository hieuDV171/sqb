package com.frozenheart.backend.modules.conversation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveGroupResponseDto {

    private Long conversationId;

    private ConversationMemberDto newLeader;

}
