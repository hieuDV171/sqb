package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu rời khỏi nhóm chat")
public class LeaveGroupRequestDto {
    @Schema(description = "ID của thành viên được chỉ định làm Tù trưởng (CHIEF) mới (chỉ dùng khi Tù trưởng hiện tại rời nhóm)", example = "3")
    private Long newChiefId;
}
