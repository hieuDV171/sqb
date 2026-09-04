package com.frozenheart.backend.modules.conversation.dto;

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
public class CreateGroupConversationDto {

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự")
    private String name;

    private String avatarUrl;

    @NotEmpty(message = "Danh sách thành viên không được để trống")
    @Size(min = 2, max = 99, message = "Nhóm phải có từ 3 đến 200 người (bao gồm người tạo và từ 2 đến 199 thành viên khác)")
    private List<Long> memberIds;
}
