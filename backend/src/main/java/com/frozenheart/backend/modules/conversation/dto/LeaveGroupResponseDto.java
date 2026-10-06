package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phản hồi khi rời nhóm chat")
public class LeaveGroupResponseDto {

    @Schema(description = "ID nhóm chat", example = "12")
    private Long conversationId;

    @Schema(description = "Thông tin Tù trưởng (CHIEF) mới kế nhiệm (nếu người rời nhóm là Tù trưởng)")
    private ConversationMemberDto newChief;
}
