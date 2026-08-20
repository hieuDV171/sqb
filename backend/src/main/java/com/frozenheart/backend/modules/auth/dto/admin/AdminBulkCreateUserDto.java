package com.frozenheart.backend.modules.auth.dto.admin;

import java.util.List;

import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.NotNull;

import com.frozenheart.backend.core.entity.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AdminBulkCreateUserDto {
    public record SingleUserImportDto(
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        @Email(message = "INVALID_PARAMETER_VALUE")
        String email,

        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        @Size(min = 8, message = "INVALID_PARAMETER_VALUE")
        String password,

        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        String fullName,

        String studentLecturerCode,

        String faculty,

        String major,

        @NotNull
        UserRole role
    ) {
    }

    public record BulkImportRequest(
        List<SingleUserImportDto> users
    ) {
    }

    public record BulkImportResult(
        int totalSuccess,
        int totalFailed,
        List<String> errors
    ) {
    }
}
