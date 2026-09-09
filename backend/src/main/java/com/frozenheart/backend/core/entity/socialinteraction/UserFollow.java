package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_follows")
public class UserFollow {

    @EmbeddedId
    private UserFollowId id;

    @Column(updatable = false)
    private Instant createdAt;

    // ------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followedUserId")
    @JoinColumn(name = "followed_user_id")
    private User followedUser;
    // -------------------------------
}
