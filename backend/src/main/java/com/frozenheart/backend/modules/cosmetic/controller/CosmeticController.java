package com.frozenheart.backend.modules.cosmetic.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import com.frozenheart.backend.modules.cosmetic.constant.CosmeticSortBy;
import com.frozenheart.backend.modules.cosmetic.dto.*;
import com.frozenheart.backend.modules.cosmetic.service.CosmeticService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cosmetics")
@RequiredArgsConstructor
public class CosmeticController {

    private final CosmeticService cosmeticService;

    @GetMapping("/my-inventory")
    public ResponseEntity<GlobalResponse<MyInventoryResponseDto>> getMyInventory(
            @RequestParam(value = "type", required = false) CosmeticType type,
            @RequestParam(value = "rarity", required = false) CosmeticRarity rarity,
            @RequestParam(value = "only_unlocked", required = false, defaultValue = "true") boolean onlyUnlocked,
            @RequestParam(value = "after", required = false) Long after,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        MyInventoryResponseDto response = cosmeticService.getMyInventory(type, rarity, onlyUnlocked, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/shop")
    public ResponseEntity<GlobalResponse<CosmeticShopResponseDto>> getShop(
            @RequestParam(value = "type", required = false) CosmeticType type,
            @RequestParam(value = "sort_by", required = false, defaultValue = "NEWEST") CosmeticSortBy sortBy,
            @RequestParam(value = "after", required = false) Long after,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        CosmeticShopResponseDto response = cosmeticService.getShop(type, sortBy, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/{id}/buy")
    public ResponseEntity<GlobalResponse<ShopItemDto>> buyCosmetic(@PathVariable("id") Long cosmeticId) {
        ShopItemDto response = cosmeticService.buyCosmetic(cosmeticId);
        return ResponseEntity.ok(GlobalResponse.success("Mua vật phẩm thành công", response));
    }

    @PutMapping("/equip")
    public ResponseEntity<GlobalResponse<EquipCosmeticResponseDto>> equipCosmetic(
            @Valid @RequestBody EquipCosmeticRequestDto request) {
        EquipCosmeticResponseDto response = cosmeticService.equipCosmetic(request);
        return ResponseEntity.ok(GlobalResponse.success("Trang bị vật phẩm thành công", response));
    }

    @PutMapping("/unequip")
    public ResponseEntity<GlobalResponse<UnequipCosmeticResponseDto>> unequipCosmetic(
            @Valid @RequestBody UnequipCosmeticRequestDto request) {
        UnequipCosmeticResponseDto response = cosmeticService.unequipCosmetic(request);
        return ResponseEntity.ok(GlobalResponse.success("Tháo vật phẩm thành công", response));
    }
}
