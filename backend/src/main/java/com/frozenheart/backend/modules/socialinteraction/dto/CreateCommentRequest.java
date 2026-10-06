package com.frozenheart.backend.modules.socialinteraction.dto;

import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Yêu cầu tạo bình luận mới")
public class CreateCommentRequest {
    @NotNull(message = "Thiếu trường targetType")
    @Schema(description = "Loại đối tượng bình luận (POST, SESSION, QUESTION)", example = "POST", requiredMode = Schema.RequiredMode.REQUIRED)
    private InteractionTargetType targetType;

    @NotNull(message = "Thiếu trường targetId")
    @Schema(description = "ID đối tượng được bình luận", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;

    @Schema(description = "Nội dung bình luận bằng văn bản", example = "Bạn có thể giải thích rõ hơn đoạn thuật toán đệ quy được không?")
    private String content;

    @Schema(description = "ID của bình luận cha nếu đây là phản hồi (reply), để trống nếu là bình luận gốc", example = "200")
    private Long parentCommentId;

    @Schema(description = "URL hình ảnh đính kèm (đã upload qua R2)", example = "https://cdn.sqb.edu.vn/sqb/comments/2026/10/solution.png")
    private String mediaUrl;
}
