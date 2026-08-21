package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record SubjectResponse(
        Long id,
        String code,
        String name
) {
}
