package com.frozenheart.backend.modules.follow.dto;

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
public class FollowResponseDto {
    private Long targetUserId;

    private boolean isFollowing;

    private int newFollowersCount;

    private LocalDateTime followedAt;
}
