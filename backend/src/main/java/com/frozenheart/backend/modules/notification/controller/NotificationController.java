package com.frozenheart.backend.modules.notification.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.notification.dto.*;
import com.frozenheart.backend.modules.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public ResponseEntity<GlobalResponse<NotificationListResponseDto>> getNotifications(
            @RequestParam(value = "after", required = false) Long after,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        NotificationListResponseDto response = notificationService.getMyNotifications(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<GlobalResponse<ReadNotificationResponseDto>> markAsRead(
            @PathVariable("id") Long notificationId) {
        ReadNotificationResponseDto response = notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<GlobalResponse<Void>> markAllAsRead() {
        notificationService.markAllNotificationsAsRead();
        return ResponseEntity.ok(GlobalResponse.success("Đã đánh dấu đọc tất cả thông báo", null));
    }

    @GetMapping("/settings/push")
    public ResponseEntity<GlobalResponse<PushSettingsResponseDto>> getPushSettings() {
        PushSettingsResponseDto response = notificationService.getPushSettings();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/settings/push")
    public ResponseEntity<GlobalResponse<PushSettingsResponseDto>> updatePushSettings(
            @Valid @RequestBody UpdatePushSettingsRequestDto request) {
        PushSettingsResponseDto response = notificationService.updatePushSettings(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
