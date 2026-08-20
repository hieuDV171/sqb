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
@Table(name = "reacts")
public class React {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String targetType;
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReactionType reactionType;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    // ------------------

}
