package com.frozenheart.backend.modules.post.dto;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật nội dung bài viết")
public class UpdatePostRequest {
    @Schema(description = "Nội dung bài viết mới", example = "Cập nhật: Mình đã tìm ra lời giải cho câu 3 rồi nhé!")
    private String content;

    @Schema(description = "Danh sách tệp tin đính kèm mới (thay thế danh sách cũ)")
    private List<MediaItem> mediaUrls;

    @NotNull(message = "Thiếu trường visibility")
    @Schema(description = "Chế độ hiển thị bài viết (PUBLIC, FRIENDS, ONLY_ME)", example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
    private PostVisibility visibility;
}
