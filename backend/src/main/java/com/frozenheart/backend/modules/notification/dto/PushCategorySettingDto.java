package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushCategorySettingDto {

    private boolean enabled;

    private String description;
}
