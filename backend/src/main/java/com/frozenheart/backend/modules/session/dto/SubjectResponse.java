package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record SubjectResponse(
        Long subjectId,
        String code,
        String name
) {
}
