package com.frozenheart.backend.modules.conversation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedMemberDto {

    private Long userId;

    private String fullName;

    private String avatarUrl;

    private String frameUrl;

    private String reason;
}
