package com.frozenheart.backend.core.dto.event;

import lombok.Builder;

@Builder
public record PointAddedEvent(Long userId, double points, Long subjectId) {

}
