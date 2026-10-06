package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo nhóm chat mới")
public class CreateGroupConversationDto {

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự")
    @Schema(description = "Tên nhóm trò chuyện", example = "Nhóm học tập Cấu trúc dữ liệu & Giải thuật", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "URL ảnh đại diện nhóm (đã upload qua R2)", example = "https://cdn.sqb.edu.vn/sqb/groups/avatar.png")
    private String avatarUrl;

    @NotEmpty(message = "Danh sách thành viên không được để trống")
    @Size(min = 2, max = 99, message = "Nhóm phải có từ 3 đến 200 người (bao gồm người tạo và từ 2 đến 199 thành viên khác)")
    @Schema(description = "Danh sách ID các thành viên mời vào nhóm (tối thiểu 2 thành viên khác người tạo)", example = "[2, 3, 4]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberIds;
}
