package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_gamifications")
public class UserGamification {

    @Id
    private Long userId;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Builder.Default
    @Column(nullable = false)
    private double publicPoints = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private double secretPoints = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private double coinBalance = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private int currentStreak = 0;

    private LocalDate lastCheckInDate;

    private Instant updatedAt;

    // ---------------------------
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    // ---------------------------
}
