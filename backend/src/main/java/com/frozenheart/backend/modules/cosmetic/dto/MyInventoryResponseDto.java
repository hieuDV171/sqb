package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Schema(description = "Kết quả truy vấn túi đồ và bộ sưu tập vật phẩm của người dùng")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyInventoryResponseDto {

    @Schema(description = "Thống kê tổng quan bộ sưu tập")
    private CosmeticSummaryDto summary;

    @Schema(description = "Danh sách các vật phẩm hiện đang được trang bị theo vị trí (slot)", example = "{\"AVATAR_FRAME\": {\"cosmeticId\": 5, \"name\": \"Khung Neon\", \"assetUrl\": \"...\"}}")
    private Map<String, EquippedItemDto> currentlyEquipped;

    @Schema(description = "Danh sách vật phẩm trong túi đồ")
    private List<InventoryItemDto> inventory;

    @Schema(description = "Thông tin phân trang dạng con trỏ (Cursor Pagination)")
    private CursorPaginationDto pagination;
}
