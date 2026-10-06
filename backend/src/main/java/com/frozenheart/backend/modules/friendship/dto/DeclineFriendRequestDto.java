package com.frozenheart.backend.modules.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Yêu cầu từ chối lời mời kết bạn")
public class DeclineFriendRequestDto {
    @NotNull(message = "Vui lòng cung cấp ID người gửi lời mời kết bạn")
    @Schema(description = "ID người đã gửi lời mời kết bạn", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long requesterId;
}
