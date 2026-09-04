package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyInventoryResponseDto {

    private CosmeticSummaryDto summary;

    private Map<String, EquippedItemDto> currentlyEquipped;

    private List<InventoryItemDto> inventory;

    private CursorPaginationDto pagination;
}
