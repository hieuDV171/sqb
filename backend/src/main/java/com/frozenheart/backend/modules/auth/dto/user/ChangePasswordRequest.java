package com.frozenheart.backend.modules.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu thay đổi mật khẩu người dùng")
public record ChangePasswordRequest(
    @Schema(description = "Mật khẩu hiện tại đang sử dụng", example = "OldPass@123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String oldPassword,

    @Schema(description = "Mật khẩu mới", example = "NewSecurePass@2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String newPassword
) {

}
