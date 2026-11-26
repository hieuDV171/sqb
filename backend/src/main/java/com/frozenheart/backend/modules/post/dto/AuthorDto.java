package com.frozenheart.backend.modules.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Thông tin công khai của tác giả; không bao gồm credential hoặc dữ liệu tài khoản riêng tư")
public class AuthorDto {
    @Schema(description = "ID tài khoản trong SQB", example = "42")
    private Long userId;
    @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
    private String fullName;
    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;
    @Schema(description = "URL khung trang trí avatar, null/rỗng khi không có", example = "https://media.example.com/frame.png")
    private String frameUrl;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Vai trò hiển thị của tác giả bài viết hoặc sự kiện Bảng tin",
            allowableValues = {"STUDENT", "LECTURER", "ADMIN", "SYSTEM"}, example = "STUDENT")
    private String role;

    public static AuthorDto fromUser(User user, UserProfile profile) {
        return AuthorDto.builder()
                .userId(user.getId())
                .fullName(profile != null ? profile.getFullName() : user.getEmail())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                .build();
    }

    public static AuthorDto fromUserWithRole(User user, UserProfile profile) {
        AuthorDto author = fromUser(user, profile);
        var role = user.getRole();
        author.setRole(role != null ? role.name() : null);
        return author;
    }

    public static AuthorDto getSystemAuthor() {
        return AuthorDto.builder()
                .userId(null)
                .fullName("Hệ thống")
                .avatarUrl("/assets/system-avatar.png")
                .frameUrl(null)
                .role("SYSTEM")
                .build();
    }
}
