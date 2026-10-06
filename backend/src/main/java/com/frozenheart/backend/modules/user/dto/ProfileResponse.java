package com.frozenheart.backend.modules.user.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frozenheart.backend.core.entity.user.Gender;
import com.frozenheart.backend.core.entity.user.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin hồ sơ chi tiết của người dùng")
public record ProfileResponse(
        @Schema(description = "ID người dùng", example = "169")
        Long userId,

        @Schema(description = "Địa chỉ email (chỉ hiển thị khi xem chính mình)", example = "hieu.dv22980@sis.hust.edu.vn")
        String email,

        @Schema(description = "Họ và tên đầy đủ", example = "Đồng Văn Hiếu")
        String fullName,

        @Schema(description = "URL ảnh đại diện", example = "https://media.sqb.edu.vn/avatar/user169.webp")
        String avatarUrl,

        @Schema(description = "URL ảnh bìa", example = "https://media.sqb.edu.vn/cover/user169.webp")
        String coverUrl,

        @Schema(description = "URL khung avatar", example = "https://media.sqb.edu.vn/frames/cyber.webp")
        String frameUrl,

        @Schema(description = "Giới tính", example = "MALE")
        Gender gender,

        @Schema(description = "Ngày tháng năm sinh (chỉ hiển thị khi xem chính mình)", example = "2004-08-05")
        LocalDate dateOfBirth,

        @Schema(description = "Tiểu sử cá nhân", example = "Sinh viên K67 Bách Khoa")
        String bio,

        @Schema(description = "Khoa / Viện / Trường thành viên", example = "Trường Công nghệ Thông tin và Truyền thông")
        String schoolFaculty,

        @Schema(description = "Chuyên ngành", example = "Khoa học máy tính")
        String major,

        @Schema(description = "Lớp sinh hoạt", example = "Khoa học máy tính 01-K67")
        String className,

        @Schema(description = "Mã số sinh viên hoặc Mã cán bộ giảng viên", example = "20221234")
        String studentLecturerCode,

        @Schema(description = "Vai trò trong hệ thống", example = "STUDENT")
        UserRole role,

        @Schema(description = "Múi giờ", example = "Asia/Ho_Chi_Minh")
        String timezone,

        @Schema(description = "Đã hoàn thành thiết lập hồ sơ", example = "true")
        boolean profileCompleted,

        @Schema(description = "Đã xác minh tài khoản", example = "true")
        boolean verified,

        // Counters & Gamification
        @Schema(description = "Tổng số câu hỏi đã đề xuất vào ngân hàng đề", example = "12")
        int totalProposedQuestions,

        @Schema(description = "Điểm tích lũy Gamification", example = "350.5")
        double gamificationPoints,

        @Schema(description = "Số lượng huy hiệu thành tích đạt được", example = "4")
        int badgesCount,

        @Schema(description = "Số lượng bạn bè", example = "28")
        int friendsCount,

        @Schema(description = "Số lượng người đang theo dõi", example = "15")
        int followersCount,

        @Schema(description = "Số lượng người mà tài khoản này đang theo dõi", example = "20")
        int followingCount,

        @Schema(description = "Thời điểm khởi tạo tài khoản")
        Instant createdAt,

        @Schema(description = "Quan hệ xã hội giữa người xem và tài khoản này (bạn bè, theo dõi...)")
        Relationship relationships
    ) {

}
