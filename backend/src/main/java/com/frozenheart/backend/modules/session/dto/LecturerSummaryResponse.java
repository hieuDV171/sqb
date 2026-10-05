package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record LecturerSummaryResponse(
        Long id,
        String fullName,
        String email,
        String studentLecturerCode,
        String schoolFaculty
) {}
