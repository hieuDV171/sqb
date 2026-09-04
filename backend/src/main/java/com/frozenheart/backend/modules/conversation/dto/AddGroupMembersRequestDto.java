package com.frozenheart.backend.modules.conversation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddGroupMembersRequestDto {

    @NotEmpty(message = "Danh sách user_ids không được để trống")
    private List<Long> userIds;
}
