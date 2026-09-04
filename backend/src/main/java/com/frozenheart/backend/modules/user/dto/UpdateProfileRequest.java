package com.frozenheart.backend.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateProfileRequest(
        @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
        String fullName,

        String avatarUrl,

        String coverUrl,

        @Size(max = 500, message = "Bio không vượt quá 500 ký tự")
        String bio
) {
}
