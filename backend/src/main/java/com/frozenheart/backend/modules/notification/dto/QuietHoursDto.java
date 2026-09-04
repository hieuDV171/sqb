package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuietHoursDto {

    private boolean enabled;

    private String startTime;

    private String endTime;

    private String timezone;
}
