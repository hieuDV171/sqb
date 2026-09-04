package com.frozenheart.backend.modules.cosmetic.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmeticSummaryDto {

    private int totalUnlocked;

    private Map<String, Integer> byRarity;

    private double collectionCompletionPercent;
}
