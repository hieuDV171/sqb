package com.frozenheart.backend.modules.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.user.UserRole;
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
@Schema(description = "Một người theo dõi kèm quan hệ hai chiều với người đang đăng nhập")
public class FollowerUserDto {
    @Schema(description = "ID tài khoản trong SQB", example = "42")
    private Long userId;

    @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
    private String fullName;

    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "URL khung trang trí avatar, null/rỗng khi không có", example = "https://media.example.com/frame.png")
    private String frameUrl;

    @Schema(description = "Vai trò tài khoản quyết định quyền truy cập nghiệp vụ")
    private UserRole role;
    @Schema(description = "Tên trường/khoa theo hồ sơ tài khoản", example = "Trường Công nghệ thông tin và Truyền thông")
    private String schoolFaculty;
    @Schema(description = "Mã sinh viên hoặc giảng viên theo hồ sơ", example = "SV20260001")
    private String code;

    @Schema(description = "Thời điểm tài khoản này bắt đầu theo dõi người gọi", example = "2026-11-19T07:00:00Z")
    private Instant followedMeAt;

    @Schema(description = "Người gọi có đang theo dõi lại tài khoản này hay không", example = "true")
    private boolean isFollowingBack;

    @Schema(description = "Hai tài khoản đã là bạn bè được chấp nhận", example = "true")
    private boolean isFriend;
}
