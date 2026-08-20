package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.session.Question;
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
@Table(name = "user_ratings")
public class UserRating {

    @EmbeddedId
    private UserRatingId id;

    private double rating;

    @Column(length = 500)
    private String comment;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ----------------------

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("ratedQuestionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_question_id")
    private Question ratedQuestion;

    // ----------------------

}
