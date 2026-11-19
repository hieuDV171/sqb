package com.frozenheart.backend.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.notification.NotificationMetadata;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông báo của người nhận hiện tại; actor/target/metadata có thể không có tùy loại thông báo")
public class NotificationDto {

    @Schema(description = "ID thông báo thuộc người nhận hiện tại", example = "42")
    private Long notificationId;

    @Schema(description = "Loại đối tượng theo enum của schema")
    private NotificationType type;

    @Schema(description = "Tiêu đề hiển thị", example = "Bộ câu hỏi ôn tập chương 1")
    private String title;

    @Schema(description = "Nội dung hiển thị của thông báo", example = "Bộ câu hỏi của bạn đã được duyệt.")
    private String body;

    @Schema(description = "URL biểu tượng thông báo khi có", example = "https://media.example.com/notification.png")
    private String iconUrl;

    @Schema(description = "Thời điểm đánh dấu đã đọc, null nếu chưa đọc", example = "2026-11-19T07:00:00Z")
    private Instant readAt;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;

    @Schema(description = "Đích điều hướng của thông báo, có thể null")
    private NotificationTargetDto target;

    @Schema(description = "Tài khoản thực hiện sự kiện, có thể không có với sự kiện hệ thống")
    private UserSummaryDto actor;

    @Schema(description = "Dữ liệu bổ sung theo loại thông báo; có thể null")
    private NotificationMetadata metadata;
}
