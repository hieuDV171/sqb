package com.frozenheart.backend.modules.cosmetic.controller;

import com.frozenheart.backend.core.annotation.Idempotent;
import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import com.frozenheart.backend.modules.cosmetic.constant.CosmeticSortBy;
import com.frozenheart.backend.modules.cosmetic.dto.*;
import com.frozenheart.backend.modules.cosmetic.service.CosmeticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "23. Cửa hàng & Vật phẩm Trang trí (Cosmetics)", description = "APIs quản lý túi đồ, cửa hàng và trang bị vật phẩm cá nhân hóa")
@RestController
@RequestMapping("/cosmetics")
@RequiredArgsConstructor
public class CosmeticController {

    private final CosmeticService cosmeticService;

    @Operation(summary = "Xem túi đồ và bộ sưu tập vật phẩm của tôi", description = "Lấy danh sách các vật phẩm trang trí đã mở khóa, phân loại theo loại/độ hiếm, cùng thống kê % hoàn thành bộ sưu tập.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin túi đồ thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực người dùng")
    })
    @GetMapping("/my-inventory")
    public ResponseEntity<GlobalResponse<MyInventoryResponseDto>> getMyInventory(
            @Parameter(description = "Lọc theo loại vật phẩm (AVATAR_FRAME, CHAT_BUBBLE, BADGE_EFFECT...)") @RequestParam(value = "type", required = false) CosmeticType type,
            @Parameter(description = "Lọc theo độ hiếm (COMMON, RARE, EPIC, LEGENDARY)") @RequestParam(value = "rarity", required = false) CosmeticRarity rarity,
            @Parameter(description = "Chỉ lấy các vật phẩm đã mở khóa", example = "true") @RequestParam(value = "only_unlocked", required = false, defaultValue = "true") boolean onlyUnlocked,
            @Parameter(description = "Con trỏ phân trang (ID vật phẩm cuối cùng của trang trước)") @RequestParam(value = "after", required = false) Long after,
            @Parameter(description = "Số lượng bản ghi mỗi trang (tối đa 50)", example = "20") @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        MyInventoryResponseDto response = cosmeticService.getMyInventory(type, rarity, onlyUnlocked, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem danh sách vật phẩm trong cửa hàng (Shop)", description = "Lấy danh sách vật phẩm đang được mở bán, kèm số dư điểm SQB Coin hiện tại của người dùng và trạng thái đã sở hữu hay chưa.")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách cửa hàng thành công")
    @GetMapping("/shop")
    public ResponseEntity<GlobalResponse<CosmeticShopResponseDto>> getShop(
            @Parameter(description = "Lọc theo loại vật phẩm (AVATAR_FRAME, CHAT_BUBBLE...)") @RequestParam(value = "type", required = false) CosmeticType type,
            @Parameter(description = "Tiêu chí sắp xếp (NEWEST, PRICE_ASC, PRICE_DESC, RARITY)") @RequestParam(value = "sort_by", required = false, defaultValue = "NEWEST") CosmeticSortBy sortBy,
            @Parameter(description = "Con trỏ phân trang (ID vật phẩm)") @RequestParam(value = "after", required = false) Long after,
            @Parameter(description = "Số lượng bản ghi mỗi trang", example = "20") @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        CosmeticShopResponseDto response = cosmeticService.getShop(type, sortBy, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Mua vật phẩm từ cửa hàng bằng SQB Coins", description = "Thực hiện mua vật phẩm. Hệ thống sẽ trừ xu từ ví SQB Coins và thêm vật phẩm vào kho đồ (hỗ trợ chống trùng lặp Idempotent trong 60s).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mua vật phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Bạn đã sở hữu vật phẩm này rồi (ACTION_ALREADY_PERFORMED)"),
            @ApiResponse(responseCode = "403", description = "Số dư ví không đủ để thanh toán (ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Vật phẩm không tồn tại (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "410", description = "Vật phẩm đã hết hạn mở bán (COSMETIC_EXPIRED)")
    })
    @Idempotent(keyPrefix = "buy_cosmetic", expireSeconds = 60)
    @PostMapping("/{id}/buy")
    public ResponseEntity<GlobalResponse<ShopItemDto>> buyCosmetic(
            @Parameter(description = "ID của vật phẩm cần mua", example = "8") @PathVariable("id") Long cosmeticId) {
        ShopItemDto response = cosmeticService.buyCosmetic(cosmeticId);
        return ResponseEntity.ok(GlobalResponse.success("Mua vật phẩm thành công", response));
    }

    @Operation(summary = "Trang bị vật phẩm trang trí", description = "Áp dụng khung avatar, bong bóng chat... vào hồ sơ cá nhân. Vật phẩm cùng loại đang đeo trước đó sẽ tự động được gỡ bỏ.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trang bị vật phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Bạn chưa sở hữu vật phẩm này trong kho đồ (COSMETIC_NOT_OWNED)")
    })
    @PutMapping("/equip")
    public ResponseEntity<GlobalResponse<EquipCosmeticResponseDto>> equipCosmetic(
            @Valid @RequestBody EquipCosmeticRequestDto request) {
        EquipCosmeticResponseDto response = cosmeticService.equipCosmetic(request);
        return ResponseEntity.ok(GlobalResponse.success("Trang bị vật phẩm thành công", response));
    }

    @Operation(summary = "Tháo gỡ vật phẩm trang trí đang dùng", description = "Tháo vật phẩm đang trang bị và phục hồi trạng thái giao diện mặc định trên trang cá nhân.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tháo vật phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Bạn chưa sở hữu vật phẩm này hoặc vật phẩm không được trang bị (COSMETIC_NOT_OWNED / ACTION_NOT_ALLOWED)")
    })
    @PutMapping("/unequip")
    public ResponseEntity<GlobalResponse<UnequipCosmeticResponseDto>> unequipCosmetic(
            @Valid @RequestBody UnequipCosmeticRequestDto request) {
        UnequipCosmeticResponseDto response = cosmeticService.unequipCosmetic(request);
        return ResponseEntity.ok(GlobalResponse.success("Tháo vật phẩm thành công", response));
    }
}
