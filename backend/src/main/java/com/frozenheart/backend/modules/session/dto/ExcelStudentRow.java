package com.frozenheart.backend.modules.session.dto;

import com.frozenheart.backend.core.entity.user.Gender;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ExcelStudentRow(
        int rowNumber,
        String studentCode,
        String fullName,
        Gender gender,
        LocalDate dateOfBirth,
        String defaultPassword,
        String email,
        String className,
        String major
) {}
