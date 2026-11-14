package com.frozenheart.backend.core.entity.socialinteraction;

import java.time.Instant;

public record ReportSnapshot(Long authorId, String content, Instant updatedAt) {}
