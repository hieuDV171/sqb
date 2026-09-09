package com.frozenheart.backend.core.dto.event;

import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushNotificationEvent {

    private List<Long> userIds;

    private String title;
    private String body;
    private String iconUrl;

    private NotificationTargetType targetType;
    private Long targetId;
    private String targetUrl;

    private PushNotificationType pushType;

    public static PushNotificationEvent single(Long userId, String title, String body, String iconUrl,
                                              NotificationTargetType targetType, Long targetId, String targetUrl,
                                              PushNotificationType pushType) {
        return PushNotificationEvent.builder()
                .userIds(userId != null ? List.of(userId) : List.of())
                .title(title)
                .body(body)
                .iconUrl(iconUrl)
                .targetType(targetType)
                .targetId(targetId)
                .targetUrl(targetUrl)
                .pushType(pushType)
                .build();
    }

    public static PushNotificationEvent batch(List<Long> userIds, String title, String body, String iconUrl,
                                             NotificationTargetType targetType, Long targetId, String targetUrl,
                                             PushNotificationType pushType) {
        return PushNotificationEvent.builder()
                .userIds(userIds)
                .title(title)
                .body(body)
                .iconUrl(iconUrl)
                .targetType(targetType)
                .targetId(targetId)
                .targetUrl(targetUrl)
                .pushType(pushType)
                .build();
    }
}
