package com.frozenheart.backend.modules.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Trạng thái và số người theo dõi sau thao tác theo dõi")
public class FollowResponseDto {
    @Schema(description = "ID tài khoản được thao tác theo dõi/bỏ theo dõi", example = "42")
    private Long targetUserId;

    @Schema(description = "Người gọi đang theo dõi tài khoản đích sau thao tác hay không", example = "true")
    private boolean isFollowing;

    @Schema(description = "Tổng người theo dõi của tài khoản đích sau thao tác", example = "3")
    private int newFollowersCount;

    @Schema(description = "Thời điểm bắt đầu theo dõi tài khoản", example = "2026-11-19T07:00:00Z")
    private Instant followedAt;
}
