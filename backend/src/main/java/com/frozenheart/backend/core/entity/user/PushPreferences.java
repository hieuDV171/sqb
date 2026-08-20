package com.frozenheart.backend.core.entity.user;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushPreferences {
    private boolean pushEnabled;

    private Map<PushNotificationType, CategorySetting> categories;

    private QuietHour quietHour;

    private boolean soundEnabled;

    private boolean vibrationEnabled;

}
