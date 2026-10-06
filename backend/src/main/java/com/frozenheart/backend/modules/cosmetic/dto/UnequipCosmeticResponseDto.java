package com.frozenheart.backend.modules.cosmetic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Schema(description = "Kết quả sau khi tháo vật phẩm trang trí")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnequipCosmeticResponseDto {

    @Schema(description = "Vị trí trang bị đã được tháo", example = "AVATAR_FRAME")
    private String slot;

    @Schema(description = "Thông tin vật phẩm vừa được tháo gỡ", example = "{\"cosmetic_id\": 5, \"name\": \"Khung Neon\"}")
    private Map<String, Object> unequippedItem;

    @Schema(description = "Các trường profile đã được xóa bỏ tương ứng", example = "{\"avatar_frame_url\": null}")
    private Map<String, String> profileUpdated;
}
