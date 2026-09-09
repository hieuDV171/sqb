package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopItemDto {

    private Long cosmeticId;

    private String name;

    private String description;

    private CosmeticType type;

    private CosmeticRarity rarity;

    private String assetUrl;

    private double price;

    private double originalPrice;

    private Instant availableUntil;

    private boolean isOwned;
}
