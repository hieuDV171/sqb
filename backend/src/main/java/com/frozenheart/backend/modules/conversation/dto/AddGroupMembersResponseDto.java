package com.frozenheart.backend.modules.conversation.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddGroupMembersResponseDto {

    private List<ConversationMemberDto> addedMembers;

    private List<FailedMemberDto> failedMembers;
}
