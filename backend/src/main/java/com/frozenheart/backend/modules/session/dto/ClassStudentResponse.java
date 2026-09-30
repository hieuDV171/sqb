package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record ClassStudentResponse(
        Long userId,
        String studentCode,
                
        String fullName,
        String email,
        String className,
        String schoolFaculty,
        String major,
        LocalDate dateOfBirth,
        Instant enrolledAt
) {}
