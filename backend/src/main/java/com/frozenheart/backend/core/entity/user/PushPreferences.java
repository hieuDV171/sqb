package com.frozenheart.backend.core.entity.user;

import com.frozenheart.backend.core.constant.Time;
import lombok.*;

import java.time.LocalTime;
import java.util.EnumMap;
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

    public static PushPreferences createDefault() {
        Map<PushNotificationType, CategorySetting> categories = new EnumMap<>(PushNotificationType.class);
        for (PushNotificationType type : PushNotificationType.values()) {
            categories.put(type, CategorySetting.builder()
                    .enabled(true)
                    .description(type.getDescription())
                    .build());
        }

        QuietHour quietHour = QuietHour.builder()
                .enabled(false)
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(7, 0))
                .timezone(Time.DEFAULT_TIMEZONE)
                .build();

        return PushPreferences.builder()
                .pushEnabled(true)
                .categories(categories)
                .quietHour(quietHour)
                .soundEnabled(true)
                .vibrationEnabled(true)
                .build();
    }
}
