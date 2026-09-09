package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.time.Instant;

@Builder
public record SemesterResponse(
                Long id,
                String name,
                boolean active,
                boolean isFinalize,
                Instant createdAt) {
}
