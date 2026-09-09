package com.frozenheart.backend.modules.auth.dto.admin;

import java.time.LocalDate;
import java.util.List;

import com.frozenheart.backend.core.entity.user.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.NotNull;

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

            Gender gender,

            @Past(message = "Ngày sinh phải là ngày trong quá khứ")
            LocalDate dateOfBirth,

            @NotNull 
            UserRoleDto role,

            String timezone
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
