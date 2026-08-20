package com.frozenheart.backend.core.entity.notification;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(value = EnumType.STRING)
    private NotificationCategory category;

    private String title;
    private String body;

    private String iconUrl;

    private String targetType;
    private Long targetId;
    private String targetUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private NotificationMetadata metadata;

    private LocalDateTime readAt;

    @Column(updatable = false)
    private LocalDateTime creadtedAt;

    // -------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;
    // -------------------------


}
