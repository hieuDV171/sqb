package com.frozenheart.backend.modules.follow.dto;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipStatsResponseDto {

    private Long userId;

    private Counts counts;

    private RelationshipWithMe relationshipWithMe;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Counts {
        private int followersCount;

        private int followingCount;

        private int friendsCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelationshipWithMe {
        private boolean isFollowing;

        private boolean isFollowedBy;

        private boolean isFriend;

        private FriendshipStatus friendRequestStatus;

        private boolean isBlocked;
    }
}
