package com.frozenheart.backend.modules.block.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUserDto {
    private Long userId;

    private String fullName;

    private String avatarUrl;

    private String frameUrl;

    private LocalDateTime blockedAt;
}
