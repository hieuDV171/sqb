package com.frozenheart.backend.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.notification.NotificationTargetType;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đích điều hướng của thông báo, phân biệt bằng type")
public class NotificationTargetDto {

    @Schema(description = "Loại đích điều hướng của thông báo")
    private NotificationTargetType type;

    @Schema(description = "ID đối tượng được tham chiếu; diễn giải cùng target_type/type của API", example = "42")
    private Long targetId;

    @Schema(description = "Đường dẫn điều hướng nội bộ nếu thông báo dùng đích URL", example = "/posts/42")
    private String url;
}
