package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thay đổi vai trò của thành viên trong nhóm")
public class UpdateMemberRoleRequestDto {

    @NotNull(message = "Role không được để trống (CHIEF, VILLAGE_ELDER hoặc VILLAGER)")
    @Schema(description = "Vai trò mới của thành viên (CHIEF, VILLAGE_ELDER, VILLAGER)", example = "VILLAGE_ELDER", allowableValues = {
            "CHIEF", "VILLAGE_ELDER", "VILLAGER" }, requiredMode = Schema.RequiredMode.REQUIRED)
    private ConversationRole role;
}
