package com.frozenheart.backend.modules.cosmetic.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquippedItemDto {

    private Long cosmeticId;

    private String name;

    private String assetUrl;
}
