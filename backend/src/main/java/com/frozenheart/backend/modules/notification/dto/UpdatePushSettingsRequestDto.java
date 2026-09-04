package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePushSettingsRequestDto {

    private boolean pushEnabled;

    private Map<String, PushCategorySettingDto> categories;

    private QuietHoursDto quietHours;

    private boolean soundEnabled;

    private boolean vibrationEnabled;
}
