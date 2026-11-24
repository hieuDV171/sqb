package com.frozenheart.backend.modules.socialgraph.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
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
@Schema(description = "Quan hệ kết bạn, hai đầu gửi/nhận và các mốc thời gian")
public class FriendshipResponseDto {
    @Schema(description = "ID người gửi lời mời kết bạn", example = "42")
    private Long requesterId;
    @Schema(description = "ID người nhận lời mời kết bạn", example = "42")
    private Long addresseeId;
    @Schema(description = "Trạng thái nghiệp vụ hiện tại theo enum của schema")
    private FriendshipStatus status;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;

    @Schema(description = "Thời điểm lời mời kết bạn được chấp nhận, null nếu chưa chấp nhận", example = "2026-11-19T07:00:00Z")
    private Instant acceptedAt;
}
