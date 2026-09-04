package com.frozenheart.backend.modules.block.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnblockResponseDto {
    private Long unblockedUserId;

    private boolean isBlocked;
}
