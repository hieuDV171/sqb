package com.frozenheart.backend.core.entity.conversation;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserParticipantId implements Serializable {

    private Long userId;

    private Long conversationId;
}
