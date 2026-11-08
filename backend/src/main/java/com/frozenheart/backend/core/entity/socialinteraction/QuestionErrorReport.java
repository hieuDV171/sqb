package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.session.Question;
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
@Table(name = "question_error_reports")
public class QuestionErrorReport {
    @EmbeddedId
    private UserRatingId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reporter;

    @MapsId("ratedQuestionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_question_id", nullable = false)
    private Question question;

    @Column(length = 500)
    private String description;

    @Column(updatable = false)
    private Instant createdAt;
}
