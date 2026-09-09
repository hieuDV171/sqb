package com.frozenheart.backend.core.entity.badge;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserBadgeId implements Serializable {

    private Long userId;

    private Long badgeId;
}
