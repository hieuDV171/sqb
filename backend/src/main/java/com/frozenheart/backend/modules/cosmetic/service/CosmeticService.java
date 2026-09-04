package com.frozenheart.backend.modules.cosmetic.service;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import com.frozenheart.backend.modules.cosmetic.constant.CosmeticSortBy;
import com.frozenheart.backend.modules.cosmetic.dto.*;

public interface CosmeticService {

    MyInventoryResponseDto getMyInventory(CosmeticType type, CosmeticRarity rarity, boolean onlyUnlocked, Long after, Integer limit);

    CosmeticShopResponseDto getShop(CosmeticType type, CosmeticSortBy sortBy, Long after, Integer limit);

    ShopItemDto buyCosmetic(Long cosmeticId);

    EquipCosmeticResponseDto equipCosmetic(EquipCosmeticRequestDto request);

    UnequipCosmeticResponseDto unequipCosmetic(UnequipCosmeticRequestDto request);
}
