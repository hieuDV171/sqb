package com.frozenheart.backend.core.entity.prediction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "minigame_llm_sessions")
public class MinigameLlmSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private int weekNumber;
    private int year;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    private Map<String, Boolean> correctAnswers; // questionId -> boolean (isLlm)

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MinigameLlmSessionStatus status;

    // ---------------------------------------

    // ---------------------------------------
    @OneToMany(mappedBy = "gameSession")
    private Set<Game6LlmQuestion> questions;
}
