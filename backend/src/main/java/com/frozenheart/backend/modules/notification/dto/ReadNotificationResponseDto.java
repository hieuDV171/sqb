package com.frozenheart.backend.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả đánh dấu đã đọc và số chưa đọc còn lại")
public class ReadNotificationResponseDto {

    @Schema(description = "ID thông báo thuộc người nhận hiện tại", example = "42")
    private Long notificationId;

    @Schema(description = "Thông báo đã được đánh dấu đã đọc", example = "true")
    private boolean isRead;

    @Schema(description = "Thời điểm đánh dấu đã đọc, null nếu chưa đọc", example = "2026-11-19T07:00:00Z")
    private Instant readAt;

    @Schema(description = "Số mục chưa đọc của người dùng hiện tại theo ngữ cảnh API", example = "3")
    private int unreadCount;
}
