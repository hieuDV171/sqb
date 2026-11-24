package com.frozenheart.backend.modules.socialgraph.block.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Trạng thái sau khi bỏ chặn một tài khoản")
public class UnblockResponseDto {
    @Schema(description = "ID tài khoản vừa được người gọi bỏ chặn", example = "42")
    private Long unblockedUserId;

    @Schema(description = "Trạng thái sau khi bỏ chặn thành công, false", example = "false")
    private boolean isBlocked;
}
