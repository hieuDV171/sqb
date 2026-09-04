package com.frozenheart.backend.modules.friendship.dto;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
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
public class FriendshipResponseDto {
    private Long requesterId;
    private Long addresseeId;
    private FriendshipStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;
}
