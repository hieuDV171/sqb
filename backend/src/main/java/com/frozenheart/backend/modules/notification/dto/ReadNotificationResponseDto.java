package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadNotificationResponseDto {

    private Long notificationId;

    private boolean isRead;

    private LocalDateTime readAt;

    private int unreadCount;
}
