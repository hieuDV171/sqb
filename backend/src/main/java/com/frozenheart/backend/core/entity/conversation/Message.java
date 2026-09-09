package com.frozenheart.backend.core.entity.conversation;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<MediaItem> mediaUrls;

    private Instant editedAt;
    private Instant deletedAt;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // ----------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    // ----------------------------

    @Singular
    @OneToMany(mappedBy = "replyToMessage")
    private Set<Message> messages;

    @Singular
    @OneToMany(mappedBy = "deletedMessageOnlyMe")
    private Set<UserDeletedMessageOnlyMe> deletedMessageUsers;

}
