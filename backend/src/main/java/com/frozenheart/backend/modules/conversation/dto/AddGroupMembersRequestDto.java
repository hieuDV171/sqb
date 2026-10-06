package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thêm thành viên vào nhóm chat")
public class AddGroupMembersRequestDto {

    @NotEmpty(message = "Danh sách user_ids không được để trống")
    @Schema(description = "Danh sách ID các người dùng cần thêm vào nhóm", example = "[5, 6, 7]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> userIds;
}
