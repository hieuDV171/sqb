package com.frozenheart.backend.modules.cosmetic.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnequipCosmeticResponseDto {

    private String slot;

    private Map<String, Object> unequippedItem;

    private Map<String, String> profileUpdated;
}
