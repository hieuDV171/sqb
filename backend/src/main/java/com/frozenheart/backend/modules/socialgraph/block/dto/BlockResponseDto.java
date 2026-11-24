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
@Schema(description = "Trạng thái sau khi chặn một tài khoản")
public class BlockResponseDto {
    @Schema(description = "ID tài khoản vừa bị người gọi chặn", example = "42")
    private Long blockedUserId;

    @Schema(description = "Trạng thái chặn sau thao tác hoặc quan hệ chặn theo ngữ cảnh API", example = "true")
    private boolean isBlocked;
}
