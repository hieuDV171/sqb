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
@Schema(description = "Yêu cầu gửi lời mời kết bạn")
public class SendFriendRequestDto {
    @NotNull(message = "Vui lòng cung cấp ID người nhận lời mời kết bạn")
    @Schema(description = "ID người nhận lời mời (phải cùng vai trò Sinh viên hoặc Giảng viên)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long addresseeId;
}
