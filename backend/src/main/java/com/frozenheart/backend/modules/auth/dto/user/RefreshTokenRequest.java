package com.frozenheart.backend.modules.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu cấp mới Access Token bằng Refresh Token")
public record RefreshTokenRequest(
    @Schema(description = "Mã Refresh Token hợp lệ còn thời hạn", example = "4fae89b2-3e41-4c77-96a9-e8548981b29d", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String refreshToken
) {

}
