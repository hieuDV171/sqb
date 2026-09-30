package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.time.Instant;

@Builder
public record SemesterResponse(
                Long semesterId,
                String name,
                boolean active,
                boolean isFinalize,
                Instant createdAt) {
}
