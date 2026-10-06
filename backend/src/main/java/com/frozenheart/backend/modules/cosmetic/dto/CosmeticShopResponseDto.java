package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(description = "Kết quả truy vấn danh sách cửa hàng vật phẩm trang trí")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmeticShopResponseDto {

    @Schema(description = "Số dư SQB Coins hiện tại của người dùng", example = "1500.0")
    private double userPoints;

    @Schema(description = "Danh sách các vật phẩm đang được mở bán trong cửa hàng")
    private List<ShopItemDto> items;

    @Schema(description = "Thông tin phân trang con trỏ (Cursor Pagination)")
    private CursorPaginationDto pagination;
}
