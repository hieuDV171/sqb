package com.frozenheart.backend.modules.post.dto;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostType;
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
@Schema(description = "Yêu cầu tạo mới bài viết thảo luận hoặc video bài giảng")
public class CreatePostRequest {
    @Schema(description = "Nội dung bài viết", example = "Mọi người cho mình hỏi bài tập 3 môn Cấu trúc dữ liệu và giải thuật với!")
    private String content;

    @Schema(description = "Danh sách tệp tin đính kèm (ảnh hoặc video)")
    private List<MediaItem> mediaUrls;

    @NotNull(message = "Visibility cannot be null")
    @Schema(description = "Chế độ hiển thị bài viết (PUBLIC, FRIENDS, ONLY_ME)", example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
    private PostVisibility visibility;

    @Schema(description = "ID môn học (Bắt buộc nếu postType là LEARNING_VIDEO dành cho Giảng viên)", example = "1")
    private Long subjectId;

    @Schema(description = "Loại bài viết (SOCIAL_POST, LEARNING_VIDEO)", example = "SOCIAL_POST")
    private PostType postType;
}
