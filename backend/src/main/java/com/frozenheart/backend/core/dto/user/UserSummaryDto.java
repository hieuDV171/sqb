package com.frozenheart.backend.core.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin công khai rút gọn của một tài khoản")
public class UserSummaryDto {

    @Schema(description = "ID tài khoản trong SQB", example = "42")
    private Long userId;

    @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
    private String fullName;

    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "URL khung trang trí avatar, null/rỗng khi không có", example = "https://media.example.com/frame.png")
    private String frameUrl;
}
