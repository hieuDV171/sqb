package com.frozenheart.backend.modules.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePushSettingsResponseDto {

    private boolean pushEnabled;

    private LocalDateTime effectiveFrom;
}
