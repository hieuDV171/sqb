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
@Table(name = "user_participants")
public class UserParticipant {

    @EmbeddedId
    private UserParticipantId id;

    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConversationRole role;

    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    private boolean isMuted;

    private LocalDateTime hiddenAt;

    private LocalDateTime lastMessageReadAt;

    // -------------------------

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message message;

    // -------------------------


}
