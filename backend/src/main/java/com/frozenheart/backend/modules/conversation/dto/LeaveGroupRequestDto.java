package com.frozenheart.backend.modules.conversation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveGroupRequestDto {
    private Long newLeaderId;
}
