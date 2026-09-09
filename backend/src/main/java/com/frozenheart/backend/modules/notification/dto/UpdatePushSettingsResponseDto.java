package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePushSettingsResponseDto {

    private boolean pushEnabled;

    private Instant effectiveFrom;
}
