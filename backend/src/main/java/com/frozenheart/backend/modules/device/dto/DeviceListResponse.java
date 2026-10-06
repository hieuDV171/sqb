package com.frozenheart.backend.modules.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Danh sách các thiết bị người dùng đang đăng nhập")
public record DeviceListResponse(
    @Schema(description = "Danh sách các thiết bị")
    List<DeviceResponse> devices
) {

}
