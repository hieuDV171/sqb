package com.frozenheart.backend.core.entity.session;

import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswer;
import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.frozenheart.backend.core.constant.EmbeddingConstants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // "Mã môn"_"Mã tác giả"_"question id"
    @Column(comment = "subjectId_authorCode_questionId")
    private String questionCode;

    @Column(columnDefinition = "TEXT")
    private String originalContent;
    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 20)
    private QuestionDifficulty difficulty;

    // @Type(PgVectorType.class)
    @Column(columnDefinition = "vector(" + EmbeddingConstants.TEXT_EMBEDDING_DIMS + ")")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] textEmbedding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<QuestionOption> originalOptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<QuestionOption> options;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 50)
    private QuestionStatus status;

    @Column(columnDefinition = "TEXT")
    private String originalExplanation;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private Double confidenceScore;
    private Double avgRating;
    private Integer ratingCount;

    private Integer reactCount;
    private Integer commentCount;

    @Column(nullable = false)
    private Integer displayOrder;

    private boolean llmGenerated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<DuplicateWarning> duplicateWarnings;

    private LocalDateTime reviewdAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ---------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;
    // ---------------------------

    @Singular
    @OneToMany(mappedBy = "question")
    private Set<QuestionMedia> ownedMedias;

    @Singular
    @OneToMany(mappedBy = "question")
    private Set<UserAnswer> answeredUsers;

    @Singular
    @OneToMany(mappedBy = "ratedQuestion")
    private Set<UserRating> ratedUsers;

}
