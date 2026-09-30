package com.frozenheart.backend.modules.auth.dto.admin;

import java.time.LocalDate;
import java.util.List;

import com.frozenheart.backend.core.entity.user.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

public class AdminBulkCreateUserDto {
    public record SingleUserImportDto(
            @NotBlank(message = "MISSING_REQUIRED_PARAMETER") 
            @Email(message = "INVALID_PARAMETER_VALUE") 
            String email,

            String password,

            @NotBlank(message = "MISSING_REQUIRED_PARAMETER") 
            String fullName,

            String studentLecturerCode,
            
            String schoolFaculty,
            String major,
            String className,

            Gender gender,

            @Past(message = "Ngày sinh phải là ngày trong quá khứ")
            LocalDate dateOfBirth,

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
