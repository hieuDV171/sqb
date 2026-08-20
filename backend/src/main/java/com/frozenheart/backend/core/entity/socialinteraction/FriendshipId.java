package com.frozenheart.backend.core.entity.socialinteraction;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FriendshipId {

    private Long senderId;

    private Long receiverId;

}
