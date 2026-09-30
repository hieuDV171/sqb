package com.frozenheart.backend.modules.notification.dto;

import com.frozenheart.backend.core.entity.notification.NotificationTargetType;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTargetDto {

    private NotificationTargetType type;

    private Long targetId;

    private String url;
}
