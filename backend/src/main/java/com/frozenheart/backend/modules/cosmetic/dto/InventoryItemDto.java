package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticAcquireMethod;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Schema(description = "Thông tin chi tiết một vật phẩm trong túi đồ của người dùng")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDto {

    @Schema(description = "ID vật phẩm", example = "12")
    private Long cosmeticId;

    @Schema(description = "Tên vật phẩm", example = "Khung Rồng Vàng Thần Thoại")
    private String name;

    @Schema(description = "Mô tả vật phẩm", example = "Khung avatar rực rỡ dành cho thủ khoa học kỳ")
    private String description;

    @Schema(description = "Loại vật phẩm trang trí", example = "AVATAR_FRAME")
    private CosmeticType type;

    @Schema(description = "Độ hiếm của vật phẩm", example = "LEGENDARY")
    private CosmeticRarity rarity;

    @Schema(description = "URL hình ảnh/tài nguyên hiển thị", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/cosmetics/frame_golden_dragon.png")
    private String assetUrl;

    @Schema(description = "Thời điểm sở hữu vật phẩm", example = "2026-10-01T08:30:00Z")
    private Instant unlockedAt;

    @Schema(description = "Phương thức đạt được vật phẩm", example = "SHOP_PURCHASE")
    private CosmeticAcquireMethod acquireMethod;

    @Schema(description = "Thời hạn sở hữu hoặc thời hạn sự kiện (null nếu vĩnh viễn)", example = "null")
    private Instant availableUntil;
}
