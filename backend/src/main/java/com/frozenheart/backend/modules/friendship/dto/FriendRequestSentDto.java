package com.frozenheart.backend.modules.friendship.dto;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
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
public class FriendRequestSentDto {
    private AuthorDto requestee;
    private FriendshipStatus status;

    private Instant createdAt;

    private Integer mutualFriendsCount;
}
