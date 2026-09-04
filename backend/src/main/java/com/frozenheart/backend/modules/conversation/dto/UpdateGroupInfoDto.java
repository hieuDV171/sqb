package com.frozenheart.backend.modules.conversation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupInfoDto {

    @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự")
    private String name;

    private String avatarUrl;
}
