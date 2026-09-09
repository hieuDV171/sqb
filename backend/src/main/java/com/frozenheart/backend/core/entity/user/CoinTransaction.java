package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coin_transactions")
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private double balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoinTransactionType type;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CoinTransactionTargetType targetType;

    private Long targetId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
