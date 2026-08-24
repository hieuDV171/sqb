package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record SemesterResponse(
        Long id,
        String name,
        boolean active,
        LocalDateTime createdAt
) {}
