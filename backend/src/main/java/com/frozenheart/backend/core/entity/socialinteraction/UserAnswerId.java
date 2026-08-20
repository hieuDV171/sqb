package com.frozenheart.backend.core.entity.socialinteraction;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class UserAnswerId {

    private Long userId;

    private Long questionId;

}
