package com.frozenheart.backend.core.entity.prediction;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "point_histories")
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private double points;
    private String reason;

    private String targetType;
    private Long targetId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    // ----------------

}
