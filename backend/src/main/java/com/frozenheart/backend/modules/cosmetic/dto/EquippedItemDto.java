package com.frozenheart.backend.modules.cosmetic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Thông tin vật phẩm đang được trang bị")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquippedItemDto {

    @Schema(description = "ID vật phẩm", example = "5")
    private Long cosmeticId;

    @Schema(description = "Tên vật phẩm", example = "Khung Avatar Cyberpunk Neon")
    private String name;

    @Schema(description = "URL hình ảnh/tài nguyên vật phẩm", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/cosmetics/frame_cyberpunk.png")
    private String assetUrl;
}
