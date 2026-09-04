package com.frozenheart.backend.modules.friendship.dto;

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
public class SendFriendRequestDto {
    @NotNull(message = "Vui lòng cung cấp ID người nhận lời mời kết bạn")
    private Long addresseeId;
}
