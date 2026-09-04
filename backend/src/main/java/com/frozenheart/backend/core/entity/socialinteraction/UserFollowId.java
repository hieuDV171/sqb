package com.frozenheart.backend.core.entity.socialinteraction;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserFollowId implements Serializable {

    private Long followerId;

    private Long followedUserId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserFollowId that = (UserFollowId) o;
        return Objects.equals(followerId, that.followerId) && Objects.equals(followedUserId, that.followedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followedUserId);
    }
}
