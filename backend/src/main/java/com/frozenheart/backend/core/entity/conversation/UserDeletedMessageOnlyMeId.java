package com.frozenheart.backend.core.entity.conversation;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserDeletedMessageOnlyMeId implements Serializable {
    private Long userId;

    private Long messageId;
}
