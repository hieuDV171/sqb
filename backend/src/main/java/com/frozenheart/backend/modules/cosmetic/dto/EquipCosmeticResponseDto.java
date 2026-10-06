package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Kết quả sau khi trang bị vật phẩm trang trí")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipCosmeticResponseDto {

    @Schema(description = "ID vật phẩm vừa được trang bị", example = "5")
    private Long cosmeticId;

    @Schema(description = "Tên vật phẩm", example = "Khung Neon Tím Siêu Cấp")
    private String name;

    @Schema(description = "Loại vật phẩm trang trí", example = "AVATAR_FRAME")
    private CosmeticType type;

    @Schema(description = "URL hình ảnh/tài nguyên vật phẩm", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/cosmetics/frame_neon_purple.png")
    private String assetUrl;

    @Schema(description = "Thời điểm trang bị", example = "2026-10-06T10:00:00Z")
    private Instant equippedAt;

    @Schema(description = "Thông tin vật phẩm cũ đã bị tháo ra (nếu có)", example = "{\"cosmetic_id\": 2, \"name\": \"Khung Cơ Bản\"}")
    private Map<String, Object> previousItem;

    @Schema(description = "Các trường profile đã được cập nhật tương ứng", example = "{\"avatar_frame_url\": \"https://sqb.s3...\"}")
    private Map<String, String> profileUpdated;
}
