package com.frozenheart.backend.modules.socialinteraction.dto;

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
@Schema(description = "Yêu cầu chỉnh sửa bình luận")
public class UpdateCommentRequest {
    @Schema(description = "Nội dung bình luận mới", example = "Cảm ơn bạn, mình đã hiểu phần giải thích rồi nhé!")
    private String content;

    @Schema(description = "URL hình ảnh mới (hoặc chuỗi rỗng để gỡ bỏ ảnh cũ)", example = "https://cdn.sqb.edu.vn/sqb/comments/2026/10/solution_v2.png")
    private String mediaUrl;
}
