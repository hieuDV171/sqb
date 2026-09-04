package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmeticShopResponseDto {

    private double userPoints;

    private List<ShopItemDto> items;

    private CursorPaginationDto pagination;
}
