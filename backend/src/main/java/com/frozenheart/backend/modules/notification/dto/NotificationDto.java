package com.frozenheart.backend.modules.notification.dto;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.notification.NotificationMetadata;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long notificationId;

    private NotificationType type;

    private String title;

    private String body;

    private String iconUrl;

    private Instant readAt;

    private Instant createdAt;

    private NotificationTargetDto target;

    private UserSummaryDto actor;

    private NotificationMetadata metadata;
}
