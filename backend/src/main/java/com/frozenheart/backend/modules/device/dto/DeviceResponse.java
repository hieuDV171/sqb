package com.frozenheart.backend.modules.device.dto;

import java.time.Instant;

import com.frozenheart.backend.core.entity.user.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin thiết bị đã đăng nhập của người dùng")
public record DeviceResponse(
        @Schema(description = "ID thiết bị trong CSDL", example = "12")
        Long userDeviceId,

        @Schema(description = "Mã định danh duy nhất của thiết bị", example = "web-browser-550e8400-e29b-41d4-a716-446655440000")
        String deviceId,

        @Schema(description = "Tên thiết bị hiển thị", example = "Chrome on Windows 11")
        String deviceName,

        @Schema(description = "Nền tảng thiết bị", example = "WEB")
        DevicePlatform platform,

        @Schema(description = "Phiên bản hệ điều hành", example = "Windows 11")
        String osVersion,

        @Schema(description = "Phiên bản ứng dụng Client", example = "1.0.0")
        String appVersion,

        @Schema(description = "Thời điểm hoạt động gần nhất")
        Instant lastActiveAt,

        @Schema(description = "Trạng thái thiết bị đang hoạt động", example = "true")
        boolean isActive,

        @Schema(description = "Có phải thiết bị hiện tại đang gửi request không", example = "true")
        boolean isCurrentDevice
    ) {

}
