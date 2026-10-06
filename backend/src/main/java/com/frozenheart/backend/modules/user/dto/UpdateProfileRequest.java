package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "Thông tin yêu cầu cập nhật hồ sơ cá nhân")
public record UpdateProfileRequest(
        @Schema(description = "Họ và tên hiển thị", example = "Đồng Văn Hiếu")
        @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
        String fullName,

        @Schema(description = "URL ảnh đại diện mới", example = "https://media.sqb.edu.vn/avatar/user169.webp")
        String avatarUrl,

        @Schema(description = "URL ảnh bìa trang cá nhân", example = "https://media.sqb.edu.vn/cover/user169.webp")
        String coverUrl,

        @Schema(description = "Tiểu sử / Giới thiệu bản thân", example = "Xách ba lô và đi!")
        @Size(max = 500, message = "Bio không vượt quá 500 ký tự")
        String bio,

        @Schema(description = "Múi giờ người dùng", example = "Asia/Ho_Chi_Minh")
        String timezone,

        @Schema(description = "Giới tính (chỉ nhận MALE hoặc FEMALE)", allowableValues = {"MALE", "FEMALE"}, example = "MALE")
        Gender gender,

        @Schema(description = "Ngày tháng năm sinh", example = "2004-08-05")
        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate dateOfBirth
) {
}
