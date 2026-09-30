package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record AssignStudentsResponse(
        int totalEnrolled,
        int alreadyEnrolledCount,
        int notFoundCount,
        List<String> notFoundCodes
) {}
