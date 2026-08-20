package com.frozenheart.backend.core.entity.conversation;

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
@Table(name = "user_deleted_message_only_mes")
public class UserDeletedMessageOnlyMe {

    @EmbeddedId
    private UserDeletedMessageOnlyMeId id;

    private LocalDateTime deletedAt;

    // --------------------------

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("messageId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_message_only_me_id")
    private Message deletedMessageOnlyMe;

    // --------------------------

}
