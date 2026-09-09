package com.frozenheart.backend.core.entity.prediction;

import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private PointHistoryReason reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PointHistoryTargetType targetType;
    private Long targetId;

    @Column(updatable = false)
    private Instant createdAt;

    // ----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;
    // ----------------

}
