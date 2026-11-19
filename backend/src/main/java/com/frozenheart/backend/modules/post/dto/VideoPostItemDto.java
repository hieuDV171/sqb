package com.frozenheart.backend.modules.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bài video trong feed, gồm metadata media và trạng thái tương tác hiện tại")
public class VideoPostItemDto {
    @Schema(description = "ID bài viết", example = "42")
    private Long postId;
    @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
    private String content;
    @Schema(description = "Phạm vi hiển thị bài viết theo enum API")
    private PostVisibility visibility;
    @Schema(description = "Tổng số lượt cảm xúc", example = "3")
    private int reactCount;
    @Schema(description = "Tổng số bình luận hiển thị theo thống kê của đối tượng", example = "3")
    private int commentCount;
    @Schema(description = "Người gọi đã thả cảm xúc vào đối tượng này", example = "true")
    private boolean reactedByMe;
    @Schema(description = "Cảm xúc hiện tại của người gọi; null nếu chưa thả cảm xúc")
    private ReactionType myReaction;
    @Schema(description = "Các tệp đính kèm; kiểu phần tử theo schema của endpoint")
    private List<MediaItem> mediaUrls;
    @Schema(description = "Thông tin công khai của tác giả")
    private AuthorDto author;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;
    @Schema(description = "Thời điểm cập nhật gần nhất, ISO-8601 UTC; có thể null nếu chưa ghi nhận", example = "2026-11-19T07:00:00Z")
    private Instant updatedAt;

    @Schema(description = "ID môn học", example = "42")
    private Long subjectId;
    @Schema(description = "Tên môn học", example = "Cơ sở dữ liệu")
    private String subjectName;
    @Schema(description = "Mã môn học", example = "IT3290")
    private String subjectCode;
}
