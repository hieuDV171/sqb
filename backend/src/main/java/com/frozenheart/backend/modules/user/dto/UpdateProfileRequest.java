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
        String bio,

        @Size(max = 100, message = "Tên khoa không vượt quá 100 ký tự") 
        String faculty,

        @Size(max = 100, message = "Tên ngành không vượt quá 100 ký tự") 
        String major,

        @Size(max = 50, message = "Mã SV/Giảng viên không vượt quá 50 ký tự") 
        String studentLecturerCode

) {

}
