package com.frozenheart.backend.modules.socialgraph.friendship.dto;

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
@Schema(description = "Thông tin một người bạn đã được chấp nhận kết nối")
public class FriendDto {
    @Schema(description = "ID tài khoản trong SQB", example = "42")
    private Long userId;
    @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
    private String fullName;
    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;
    @Schema(description = "URL khung trang trí avatar, null/rỗng khi không có", example = "https://media.example.com/frame.png")
    private String frameUrl;

    @Schema(description = "Thời điểm hai tài khoản trở thành bạn bè", example = "2026-11-19T07:00:00Z")
    private Instant friendsSince;

    @Schema(description = "Số người bạn chung giữa người gọi và tài khoản này", example = "3")
    private Integer mutualFriendsCount;
}
