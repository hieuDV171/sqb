package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tài khoản không được thêm vào nhóm và lý do thất bại tương ứng")
public class FailedMemberDto {

    @Schema(description = "ID tài khoản trong SQB", example = "42")
    private Long userId;

    @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
    private String fullName;

    @Schema(description = "URL ảnh đại diện công khai; có thể null/rỗng khi chưa đặt ảnh", example = "https://media.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "URL khung trang trí avatar, null/rỗng khi không có", example = "https://media.example.com/frame.png")
    private String frameUrl;

    @Schema(description = "Lý do thao tác hoặc lý do thất bại của mục tương ứng", example = "Nội dung không phù hợp quy định cộng đồng")
    private String reason;
}
