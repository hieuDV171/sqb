package com.frozenheart.backend.modules.cosmetic.dto;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipCosmeticResponseDto {

    private Long cosmeticId;

    private String name;

    private CosmeticType type;

    private String assetUrl;

    private Instant equippedAt;

    private Map<String, Object> previousItem;

    private Map<String, String> profileUpdated;
}
