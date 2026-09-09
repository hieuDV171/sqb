package com.frozenheart.backend.modules.friendship.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDto {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String frameUrl;

    private Instant friendsSince;

    private Integer mutualFriendsCount;
}
