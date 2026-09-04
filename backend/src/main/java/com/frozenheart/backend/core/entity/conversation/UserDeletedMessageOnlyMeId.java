package com.frozenheart.backend.core.entity.conversation;

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
public class UserDeletedMessageOnlyMeId implements Serializable {
    private Long userId;

    private Long messageId;
}
