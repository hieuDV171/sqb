package com.frozenheart.backend.modules.device.dto;

import java.util.List;

public record DeviceListResponse(
    List<DeviceResponse> devices
) {

}
