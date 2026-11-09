package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record Relationship(
        boolean isFriend,
        @Schema(description = "Người được xem hồ sơ đang theo dõi người đăng nhập")
        boolean isFollowing,
        @Schema(description = "Người đăng nhập đang theo dõi người được xem hồ sơ")
        boolean isFollowed,
        FriendshipStatus friendRequestStatus
    ) {

}
