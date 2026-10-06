package com.frozenheart.backend.modules.auth.dto.admin;

import com.frozenheart.backend.core.entity.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Thông tin phản hồi xác thực và bộ mã truy cập")
public record AuthResponse(
        @Schema(description = "ID người dùng trong hệ thống", example = "101")
        Long userId,

        @Schema(description = "Tên đăng nhập / Email của người dùng", example = "hieu.dv224980@sis.hust.edu.vn")
        String username,

        @Schema(description = "Vai trò người dùng trong hệ thống", example = "STUDENT")
        UserRole role,

        @Schema(description = "JWT Access Token dùng cho các request tiếp theo", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,

        @Schema(description = "Refresh Token dùng để cấp mới Access Token", example = "4fae89b2-3e41-4c77-96a9-e8548981b29d")
        String refreshToken,

        @Schema(description = "Đường dẫn ảnh đại diện", example = "https://media.sqb.edu.vn/avatar/user101.webp")
        String avatarUrl,

        @Schema(description = "Đường dẫn ảnh bìa", example = "https://media.sqb.edu.vn/cover/user101.webp")
        String coverUrl,

        @Schema(description = "Đường dẫn khung ảnh đại diện", example = "https://media.sqb.edu.vn/frames/gold.webp")
        String frameUrl,

        @Schema(description = "Trạng thái đã xác minh tài khoản", example = "true")
        boolean verified,

        @Schema(description = "Trạng thái đã hoàn tất cập nhật hồ sơ cá nhân", example = "true")
        boolean profileCompleted

) {
}
