package com.frozenheart.backend.core.dto.event;

import java.util.List;

public record AccountDeletedEvent(List<Long> userIds) {
}
