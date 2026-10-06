package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Schema(description = "Thông tin chi tiết một vật phẩm được bày bán trong cửa hàng")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopItemDto {

    @Schema(description = "ID vật phẩm", example = "8")
    private Long cosmeticId;

    @Schema(description = "Tên vật phẩm", example = "Bong Bóng Chat Thần Sấm")
    private String name;

    @Schema(description = "Mô tả vật phẩm", example = "Khung hội thoại với hiệu ứng tia sét khi gửi tin nhắn")
    private String description;

    @Schema(description = "Loại vật phẩm trang trí", example = "CHAT_BUBBLE")
    private CosmeticType type;

    @Schema(description = "Độ hiếm của vật phẩm", example = "EPIC")
    private CosmeticRarity rarity;

    @Schema(description = "URL hình ảnh/tài nguyên hiển thị", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/cosmetics/bubble_thunder.png")
    private String assetUrl;

    @Schema(description = "Giá bán hiện tại (SQB Coins)", example = "250.0")
    private double price;

    @Schema(description = "Giá gốc trước khi giảm giá (nếu có)", example = "300.0")
    private double originalPrice;

    @Schema(description = "Thời hạn mở bán (null nếu bán vĩnh viễn)", example = "2026-12-31T23:59:59Z")
    private Instant availableUntil;

    @Schema(description = "Người dùng hiện tại đã sở hữu vật phẩm này chưa", example = "false")
    private boolean isOwned;
}
