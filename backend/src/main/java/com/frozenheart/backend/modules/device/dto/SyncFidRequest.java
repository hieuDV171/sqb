package com.frozenheart.backend.modules.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SyncFidRequest(
        @Schema(description = "Firebase Installation ID (FID) cần đồng bộ hoặc cập nhật", example = "fid_example_xyz", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "FID không được để trống")
        String fid
) {}
