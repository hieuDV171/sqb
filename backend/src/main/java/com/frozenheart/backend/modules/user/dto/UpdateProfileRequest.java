package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.user.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UpdateProfileRequest(
        @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
        String fullName,

        String avatarUrl,

        String coverUrl,

        @Size(max = 500, message = "Bio không vượt quá 500 ký tự")
        String bio,

        String timezone,

        Gender gender,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate dateOfBirth
) {
}
