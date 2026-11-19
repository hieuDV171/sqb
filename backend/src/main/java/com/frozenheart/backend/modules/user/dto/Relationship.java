package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Quan hệ bạn bè/theo dõi giữa người gọi và hồ sơ đang xem")
public record Relationship(
        @Schema(description = "Hai tài khoản đã là bạn bè được chấp nhận", example = "true")
        boolean isFriend,
        @Schema(description = "Người được xem hồ sơ đang theo dõi người đăng nhập", example = "true")
        boolean isFollowing,
        @Schema(description = "Người đăng nhập đang theo dõi người được xem hồ sơ", example = "true")
        boolean isFollowed,
        @Schema(description = "Trạng thái lời mời kết bạn giữa hai tài khoản; null nếu không có")
        FriendshipStatus friendRequestStatus
    ) {

}
