package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadNotificationResponseDto {

    private Long notificationId;

    private boolean isRead;

    private Instant readAt;

    private int unreadCount;
}
