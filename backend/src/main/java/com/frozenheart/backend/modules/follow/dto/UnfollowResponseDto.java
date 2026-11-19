package com.frozenheart.backend.modules.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Trạng thái và số người theo dõi sau thao tác bỏ theo dõi")
public class UnfollowResponseDto {
    @Schema(description = "ID tài khoản được thao tác theo dõi/bỏ theo dõi", example = "42")
    private Long targetUserId;

    @Schema(description = "Trạng thái sau khi bỏ theo dõi thành công, false", example = "false")
    private boolean isFollowing;

    @Schema(description = "Tổng người theo dõi của tài khoản đích sau thao tác", example = "3")
    private int newFollowersCount;
}
