package com.frozenheart.backend.modules.notification.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.notification.dto.*;
import com.frozenheart.backend.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "11. Thông báo (Notifications)", description = "Các API đọc thông báo người dùng, đánh dấu đã đọc và cấu hình tùy chọn nhận thông báo đẩy (Push Notifications, Quiet Hours)")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lấy danh sách thông báo của tôi", description = "Trả về danh sách thông báo kèm số lượng thông báo chưa đọc, hỗ trợ phân trang con trỏ (cursor after).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thông báo thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/notifications")
    public ResponseEntity<GlobalResponse<NotificationListResponseDto>> getNotifications(
            @Parameter(description = "Con trỏ ID thông báo để lấy tiếp", example = "105")
            @RequestParam(value = "after", required = false) Long after,
            @Parameter(description = "Số lượng thông báo mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        NotificationListResponseDto response = notificationService.getMyNotifications(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đánh dấu một thông báo đã đọc", description = "Cập nhật trạng thái đã đọc cho thông báo theo ID và tính toán lại số lượng chưa đọc còn lại.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đánh dấu đọc thông báo thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Bạn không có quyền truy cập thông báo này (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông báo (RESOURCE_NOT_FOUND)")
    })
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<GlobalResponse<ReadNotificationResponseDto>> markAsRead(
            @Parameter(description = "ID thông báo", example = "88", required = true)
            @PathVariable("id") Long notificationId) {
        ReadNotificationResponseDto response = notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc", description = "Chuyển toàn bộ thông báo chưa đọc của người dùng hiện tại sang trạng thái đã đọc tức thì.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã đánh dấu đọc tất cả thông báo"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @PostMapping("/notifications/read-all")
    public ResponseEntity<GlobalResponse<Void>> markAllAsRead() {
        notificationService.markAllNotificationsAsRead();
        return ResponseEntity.ok(GlobalResponse.success("Đã đánh dấu đọc tất cả thông báo", null));
    }

    @Operation(summary = "Lấy cấu hình nhận thông báo đẩy", description = "Xem tùy chọn bật/tắt từng danh mục thông báo (bình luận, tin nhắn, bạn bè...) và khung giờ yên tĩnh (Quiet Hours).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy cài đặt thông báo thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/settings/push")
    public ResponseEntity<GlobalResponse<PushSettingsResponseDto>> getPushSettings() {
        PushSettingsResponseDto response = notificationService.getPushSettings();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Cập nhật cấu hình nhận thông báo đẩy", description = "Bật/tắt các danh mục thông báo và điều chỉnh khung giờ yên tĩnh không làm phiền.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật cấu hình thông báo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)")
    })
    @PutMapping("/settings/push")
    public ResponseEntity<GlobalResponse<PushSettingsResponseDto>> updatePushSettings(
            @Valid @RequestBody UpdatePushSettingsRequestDto request) {
        PushSettingsResponseDto response = notificationService.updatePushSettings(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
