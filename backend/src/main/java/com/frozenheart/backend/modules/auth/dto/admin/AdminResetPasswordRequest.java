package com.frozenheart.backend.modules.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu khôi phục mật khẩu người dùng từ Quản trị viên")
public record AdminResetPasswordRequest(
        @Schema(description = "Email của tài khoản cần đặt lại mật khẩu", example = "hieu.dv224980@sis.hust.edu.vn", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER") 
        @Email(message = "INVALID_PARAMETER_VALUE") 
        String email,

        @Schema(description = "Mật khẩu mới chỉ định (để trống nếu muốn khôi phục về mật khẩu mặc định của trường)", example = "20224980")
        String newPassword
) {

}
