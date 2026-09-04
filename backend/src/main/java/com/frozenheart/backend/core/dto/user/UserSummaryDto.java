package com.frozenheart.backend.core.dto.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

    private Long userId;

    private String fullName;

    private String avatarUrl;

    private String frameUrl;
}
