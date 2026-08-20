package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_blocks")
public class UserBlock {

    @EmbeddedId
    private UserBlockId id;


    private String reason;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ----------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blockerId")
    @JoinColumn(name = "blocker_id")
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blockedId")
    @JoinColumn(name = "blocked_id")
    private User blocked;
    // ------------------------------
}
