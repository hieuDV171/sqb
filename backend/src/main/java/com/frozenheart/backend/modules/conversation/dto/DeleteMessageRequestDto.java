package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.modules.conversation.constant.MessageDeleteScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu xóa hoặc thu hồi tin nhắn")
public class DeleteMessageRequestDto {

    @NotNull(message = "Scope không được để trống (ME hoặc EVERYONE)")
    @Schema(description = "Phạm vi xóa tin nhắn: ME (chỉ ẩn ở phía tôi) hoặc EVERYONE (thu hồi với mọi người)", example = "EVERYONE", allowableValues = {"ME", "EVERYONE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private MessageDeleteScope scope;
}
