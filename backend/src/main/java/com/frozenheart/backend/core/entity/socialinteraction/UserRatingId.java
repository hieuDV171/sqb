package com.frozenheart.backend.core.entity.socialinteraction;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserRatingId {

    private Long userId;

    private Long ratedQuestionId;

}
