package com.frozenheart.backend.modules.device.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.device.dto.DeviceListResponse;
import com.frozenheart.backend.modules.device.service.DeviceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/{deviceId}/logout")
    public ResponseEntity<GlobalResponse<Void>> revokeDevice(@PathVariable String deviceId) {
        deviceService.revokeDevice(deviceId);

        return ResponseEntity.ok(GlobalResponse.success());
    }
    
    @GetMapping
    public ResponseEntity<GlobalResponse<DeviceListResponse>> getMyDevices() {
        DeviceListResponse response = deviceService.getMyDevices();
        
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<GlobalResponse<Void>> deleteDevice(@PathVariable String deviceId) {
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
