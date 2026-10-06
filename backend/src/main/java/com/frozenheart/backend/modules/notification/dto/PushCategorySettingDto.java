package com.frozenheart.backend.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cấu hình chi tiết cho một danh mục thông báo")
public class PushCategorySettingDto {

    @Schema(description = "Bật/tắt thông báo cho danh mục này", example = "true")
    private boolean enabled;

    @Schema(description = "Mô tả ý nghĩa của danh mục thông báo", example = "Thông báo khi có người bình luận vào bài viết của bạn")
    private String description;
}
