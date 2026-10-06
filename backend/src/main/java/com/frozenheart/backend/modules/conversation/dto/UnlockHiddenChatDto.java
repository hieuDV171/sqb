package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu mở khóa kho ẩn trò chuyện")
public class UnlockHiddenChatDto {

    @NotBlank(message = "Mã PIN không được để trống")
    @Schema(description = "Mã PIN 6 chữ số đã thiết lập", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;
}
