package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.modules.conversation.constant.MessageDeleteScope;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả xóa tin nhắn và phạm vi áp dụng")
public class DeleteMessageResponseDto {

    @Schema(description = "ID tin nhắn", example = "42")
    private Long messageId;

    @Schema(description = "Phạm vi xóa tin nhắn: theo enum API, chỉ phía người gọi hoặc mọi người")
    private MessageDeleteScope scope;

    @Schema(description = "Thông tin giải thích kết quả thao tác", example = "Đã xóa tin nhắn trong phạm vi được chọn")
    private String note;
}
