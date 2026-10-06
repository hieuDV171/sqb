package com.frozenheart.backend.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cấu hình khung giờ yên tĩnh")
public class QuietHoursDto {

    @Schema(description = "Bật/tắt chế độ khung giờ yên tĩnh", example = "true")
    private boolean enabled;

    @Schema(description = "Giờ bắt đầu chế độ yên tĩnh (định dạng HH:mm)", example = "22:00")
    private String startTime;

    @Schema(description = "Giờ kết thúc chế độ yên tĩnh (định dạng HH:mm)", example = "07:00")
    private String endTime;
}
