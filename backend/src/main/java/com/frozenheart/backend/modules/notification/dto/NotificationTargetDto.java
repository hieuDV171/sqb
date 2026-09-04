package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTargetDto {

    private String type;

    private Long id;

    private String url;
}
