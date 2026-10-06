package com.frozenheart.backend.modules.notification.dto;

import com.frozenheart.backend.core.entity.user.PushNotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật cấu hình thông báo đẩy")
public class UpdatePushSettingsRequestDto {

    @Schema(description = "Bật/tắt toàn bộ thông báo đẩy", example = "true")
    private boolean pushEnabled;

    @Schema(description = "Cấu hình chi tiết theo từng danh mục thông báo cố định (SOCIAL, ACADEMIC, GAMIFICATION, SYSTEM)")
    private Map<PushNotificationType, PushCategorySettingDto> categories;

    @Schema(description = "Cấu hình khung giờ yên tĩnh không làm phiền")
    private QuietHoursDto quietHours;

    @Schema(description = "Bật/tắt âm thanh thông báo", example = "true")
    private boolean soundEnabled;

    @Schema(description = "Bật/tắt rung khi có thông báo", example = "true")
    private boolean vibrationEnabled;
}
