package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import lombok.Builder;

@Builder
public record Relationship(
        boolean isFriend,
        boolean isFollowing,
        boolean isFollowed,
        FriendshipStatus friendRequestStatus
    ) {

}
