package com.frozenheart.backend.modules.cosmetic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipCosmeticRequestDto {

    @NotNull(message = "ID vật phẩm không được để trống")
    private Long cosmeticId;
}
