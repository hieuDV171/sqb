package com.frozenheart.backend.modules.cosmetic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Yêu cầu trang bị vật phẩm trang trí")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipCosmeticRequestDto {

    @Schema(description = "ID của vật phẩm muốn trang bị", example = "5")
    @NotNull(message = "ID vật phẩm không được để trống")
    private Long cosmeticId;
}
