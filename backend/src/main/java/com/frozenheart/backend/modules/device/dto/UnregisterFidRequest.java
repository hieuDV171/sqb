package com.frozenheart.backend.modules.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Gỡ Firebase Installation ID khỏi thiết bị của chính tài khoản")
public record UnregisterFidRequest(
        @Schema(description = "Firebase Installation ID (FID) cần hủy đăng ký", example = "fid_example_xyz", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        String fid
) {
}
