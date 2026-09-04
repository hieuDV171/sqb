package com.frozenheart.backend.modules.follow.dto;

import com.frozenheart.backend.core.entity.user.UserRole;
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
public class FollowingUserDto {
    private Long userId;

    private String fullName;

    private String avatarUrl;

    private String frameUrl;

    private UserRole role;
    private String faculty;
    private String code;

    private LocalDateTime followedAt;

    private boolean isFollowingMe;

    private boolean isFriend;
}
