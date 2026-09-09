package com.frozenheart.backend.core.entity.prediction;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PredictionTargetType targetType;

    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private PredictionData predictionData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private PredictionData actualData;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PredictionStatus status;

    private boolean isCorrect;

    private Instant targetDate;
    private Instant resolvedAt;

    @Column(updatable = false)
    private Instant createdAt;

    // ---------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gambler_id")
    private User gambler;
    // ---------------------

}
