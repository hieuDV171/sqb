package com.frozenheart.backend.core.entity.conversation;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserParticipantId implements Serializable {

    private Long userId;

    private Long conversationId;
}
