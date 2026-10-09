package com.frozenheart.backend.modules.device.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.device.dto.DeviceListResponse;
import com.frozenheart.backend.modules.device.service.DeviceService;
import com.frozenheart.backend.modules.device.dto.SyncFidRequest;
import com.frozenheart.backend.modules.device.dto.UnregisterFidRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "28. Quản lý Thiết bị (Devices)", description = "Các API kiểm tra danh sách thiết bị đang đăng nhập và thu hồi phiên từ xa")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Đăng xuất thiết bị chỉ định", description = "Thu hồi quyền truy cập và xóa Refresh Token của thiết bị theo deviceId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thu hồi phiên thiết bị thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị (DEVICE_NOT_FOUND)")
    })
    @PostMapping("/{deviceId}/logout")
    public ResponseEntity<GlobalResponse<Void>> revokeDevice(
            @Parameter(description = "Mã định danh duy nhất của thiết bị cần đăng xuất", example = "web-browser-550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable String deviceId
    ) {
        deviceService.revokeDevice(deviceId);

        return ResponseEntity.ok(GlobalResponse.success());
    }
    
    @Operation(summary = "Lấy danh sách thiết bị đang đăng nhập", description = "Trả về toàn bộ các thiết bị (Web, Mobile) đã từng đăng nhập tài khoản này kèm trạng thái hoạt động gần nhất.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thiết bị thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<DeviceListResponse>> getMyDevices() {
        DeviceListResponse response = deviceService.getMyDevices();
        
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xóa thông tin thiết bị khỏi tài khoản", description = "Xóa vĩnh viễn bản ghi thiết bị trong hệ thống.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thiết bị thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị (DEVICE_NOT_FOUND)")
    })
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<GlobalResponse<Void>> deleteDevice(
            @Parameter(description = "Mã định danh duy nhất của thiết bị cần xóa", example = "web-browser-550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable String deviceId
    ) {
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Hủy đăng ký Firebase Installation ID (FID)", description = "Vô hiệu hóa thiết bị và xóa FID khi người dùng tắt quyền thông báo hoặc sự kiện onUnregistered xảy ra.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hủy đăng ký FID thành công")
    })
    @PostMapping("/fid/unregister")
    public ResponseEntity<GlobalResponse<Void>> unregisterFid(
            @Valid @RequestBody UnregisterFidRequest request
    ) {
        deviceService.unregisterFid(request.fid());
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Đồng bộ Firebase Installation ID (FID)", description = "Cập nhật hoặc liên kết FID mới cho thiết bị hiện tại của người dùng đang đăng nhập.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đồng bộ FID thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @PostMapping("/fid")
    public ResponseEntity<GlobalResponse<Void>> syncFid(
            @Valid @RequestBody SyncFidRequest request
    ) {
        deviceService.syncFid(request.fid());
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
