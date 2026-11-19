package com.frozenheart.backend.modules.auth.dto.admin;

import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.security.AccountState;
import com.frozenheart.backend.core.security.TokenType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
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
        @Schema(description = "JSON chỉ có RT ở native hoặc nhánh LEGACY_JSON tạm thời khi bật. Luồng cookie WEB luôn null. ONBOARDING không có refresh token.", example = "eyJhbGciOiJIUzUxMiJ9...")
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
        boolean profileCompleted,
        @Schema(description = "Loại token được cấp: ACCESS hoặc ONBOARDING", example = "ACCESS")
        TokenType tokenType,
        @Schema(description = "Trạng thái thiết lập tài khoản hiện tại", example = "ACTIVE")
        AccountState accountState

) {
}
