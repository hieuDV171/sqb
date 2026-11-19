package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả thêm thành viên: người thêm trực tiếp, người thất bại và số lời mời đã gửi")
public class AddGroupMembersResponseDto {

    @Schema(description = "Các thành viên đã được thêm trực tiếp vào nhóm")
    private List<ConversationMemberDto> addedMembers;

    @Schema(description = "Các tài khoản không thêm được kèm lý do riêng")
    private List<FailedMemberDto> failedMembers;

    @Schema(description = "Số tài khoản được gửi lời mời thay vì thêm trực tiếp", example = "3")
    private int invitedMemberCount;
}
