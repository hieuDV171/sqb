package com.frozenheart.backend.modules.notification.dto;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

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

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    private NotificationTargetDto target;

    private UserSummaryDto actor;
}
