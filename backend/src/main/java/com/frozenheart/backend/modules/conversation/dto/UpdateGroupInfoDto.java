package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật thông tin nhóm trò chuyện")
public class UpdateGroupInfoDto {

    @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự")
    @Schema(description = "Tên nhóm mới", example = "Nhóm thảo luận Ôn thi Cuối kỳ Giải tích 1")
    private String name;

    @Schema(description = "URL ảnh đại diện mới của nhóm (đã upload qua R2)", example = "https://cdn.sqb.edu.vn/sqb/groups/avatar_v2.png")
    private String avatarUrl;
}
