package com.frozenheart.backend.core.entity.prediction;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.modules.gamification.dto.BankType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "game6_llm_questions")
public class Game6LlmQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long originalQuestionId;

    @Column(nullable = false, length = 20)
    private BankType sourceType; // 'CURRENT' or 'LEGACY'

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    private List<QuestionOption> options;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<String> imageUrls;

    private boolean llmGenerated;

    @Column(updatable = false)
    private LocalDateTime createdAt;
}
