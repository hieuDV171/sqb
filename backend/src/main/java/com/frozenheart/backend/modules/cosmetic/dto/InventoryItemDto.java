package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticAcquireMethod;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDto {

    private Long cosmeticId;

    private String name;

    private String description;

    private CosmeticType type;

    private CosmeticRarity rarity;

    private String assetUrl;

    private LocalDateTime unlockedAt;

    private CosmeticAcquireMethod acquireMethod;

    private LocalDateTime availableUntil;
}
