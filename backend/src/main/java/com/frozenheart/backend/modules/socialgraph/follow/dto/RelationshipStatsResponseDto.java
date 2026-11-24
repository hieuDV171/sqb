package com.frozenheart.backend.modules.socialgraph.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
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
@Schema(description = "Thống kê quan hệ của một tài khoản và trạng thái quan hệ với người gọi")
public class RelationshipStatsResponseDto {

    @Schema(description = "ID tài khoản trong SQB", example = "42")
    private Long userId;

    @Schema(description = "Các số liệu quan hệ của tài khoản được xem")
    private Counts counts;

    @Schema(description = "Trạng thái quan hệ giữa người đang đăng nhập và tài khoản được xem")
    private RelationshipWithMe relationshipWithMe;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Số bạn bè, người theo dõi và người đang theo dõi")
    public static class Counts {
        @Schema(description = "Số tài khoản đang theo dõi người dùng này", example = "3")
        private int followersCount;

        @Schema(description = "Số tài khoản người dùng này đang theo dõi", example = "3")
        private int followingCount;

        @Schema(description = "Số quan hệ bạn bè đã được chấp nhận", example = "3")
        private int friendsCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Trạng thái quan hệ giữa người gọi và tài khoản đang xem")
    public static class RelationshipWithMe {
        @Schema(description = "Người gọi đang theo dõi tài khoản đích sau thao tác hay không", example = "true")
        private boolean isFollowing;

        @Schema(description = "Tài khoản được xem đang theo dõi người gọi", example = "true")
        private boolean isFollowedBy;

        @Schema(description = "Hai tài khoản đã là bạn bè được chấp nhận", example = "true")
        private boolean isFriend;

        @Schema(description = "Trạng thái lời mời kết bạn giữa hai tài khoản; null nếu không có")
        private FriendshipStatus friendRequestStatus;

        @Schema(description = "Trạng thái chặn sau thao tác hoặc quan hệ chặn theo ngữ cảnh API", example = "true")
        private boolean isBlocked;
    }
}
