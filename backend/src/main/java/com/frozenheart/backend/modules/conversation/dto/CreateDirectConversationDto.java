package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo cuộc trò chuyện trực tiếp 1-1")
public class CreateDirectConversationDto {

    @NotNull(message = "target_user_id không được để trống")
    @Schema(description = "ID của người dùng cần nhắn tin 1-1", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetUserId;
}
