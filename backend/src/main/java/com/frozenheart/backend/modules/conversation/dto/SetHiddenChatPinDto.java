package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cài đặt hoặc thay đổi mã PIN kho ẩn trò chuyện")
public class SetHiddenChatPinDto {

    @NotBlank(message = "Mã PIN không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã PIN phải có đúng 6 chữ số")
    @Schema(description = "Mã PIN mới gồm đúng 6 chữ số", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Pattern(regexp = "^[0-9]{6}$", message = "Mã PIN cũ phải có đúng 6 chữ số")
    @Schema(description = "Mã PIN cũ gồm đúng 6 chữ số (bắt buộc khi đổi PIN)", example = "000000")
    private String oldPin;
}
