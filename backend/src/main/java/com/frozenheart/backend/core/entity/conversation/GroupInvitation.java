package com.frozenheart.backend.core.entity.conversation;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "group_invitations",
        uniqueConstraints = @UniqueConstraint(name = "uq_group_invitation_conversation_invitee",
                columnNames = {"conversation_id", "invitee_id"}),
        indexes = @Index(name = "idx_group_invitation_inbox", columnList = "invitee_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Column(nullable = false)
    private Instant expiresAt;

    public GroupInvitation(Conversation conversation, User inviter, User invitee, Instant expiresAt) {
        this.conversation = conversation;
        this.inviter = inviter;
        this.invitee = invitee;
        this.expiresAt = expiresAt;
    }
}
