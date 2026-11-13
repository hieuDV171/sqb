package com.frozenheart.backend.modules.user.dto;

import java.time.Instant;

public record AccountDeletionResponse(Instant scheduledFor, boolean canCancel) {
}
