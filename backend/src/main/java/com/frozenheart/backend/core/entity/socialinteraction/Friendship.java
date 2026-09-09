package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "friendships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

    @EmbeddedId
    private FriendshipId id;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FriendshipStatus status;

    private Instant acceptedAt;

    @Column(updatable = false)
    private Instant createdAt;

    // ------------------------

    @MapsId("senderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @MapsId("receiverId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    // ------------------------

}
