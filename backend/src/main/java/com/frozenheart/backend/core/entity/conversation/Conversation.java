package com.frozenheart.backend.core.entity.conversation;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConversationType type;

    private String name;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String description;

    private Instant lastMessageAt;

    @Column(updatable = false)
    private Instant creadtedAt;

    private Instant updatedAt;

    // ----------------------------
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private Message lastMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creater_id")
    private User creater;

    // ----------------------------
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserParticipant> participants;

}
