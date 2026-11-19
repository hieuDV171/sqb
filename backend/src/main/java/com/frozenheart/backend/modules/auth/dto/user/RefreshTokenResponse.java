package com.frozenheart.backend.modules.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Thông tin phản hồi cấp mới bộ token truy cập")
public record RefreshTokenResponse(
        @Schema(description = "JWT Access Token mới", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,
        @Schema(description = "JWT RT mới cho native/LEGACY_JSON tạm thời; cookie WEB trả null và nhận RT qua Set-Cookie", example = "eyJhbGciOiJIUzUxMiJ9...")
        String refreshToken
) {

}
