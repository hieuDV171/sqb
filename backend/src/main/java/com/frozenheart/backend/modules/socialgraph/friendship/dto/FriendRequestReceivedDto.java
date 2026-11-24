package com.frozenheart.backend.modules.socialgraph.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
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
@Schema(description = "Lời mời kết bạn nhận được từ một tài khoản khác")
public class FriendRequestReceivedDto {
    @Schema(description = "Tài khoản gửi lời mời kết bạn")
    private AuthorDto requester;
    @Schema(description = "Trạng thái nghiệp vụ hiện tại theo enum của schema")
    private FriendshipStatus status;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;

    @Schema(description = "Số người bạn chung giữa người gọi và tài khoản này", example = "3")
    private Integer mutualFriendsCount;
}
