package com.frozenheart.backend.modules.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Thông tin phản hồi cấp mới bộ token truy cập")
public record RefreshTokenResponse(
        @Schema(description = "JWT Access Token mới", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,

        @Schema(description = "Refresh Token mới", example = "6cba92d1-4e12-4d88-87a1-f76123456789")
        String refreshToken
) {

}
