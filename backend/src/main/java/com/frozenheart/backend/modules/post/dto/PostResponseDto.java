package com.frozenheart.backend.modules.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostType;
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
@Schema(description = "Bài viết đầy đủ cùng media, tác giả và trạng thái tương tác của người đang đăng nhập")
public class PostResponseDto {
    @Schema(description = "ID bài viết", example = "42")
    private Long postId;
    @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
    private String content;
    @Schema(description = "Loại bài viết, quyết định các trường nghiệp vụ liên quan")
    private PostType postType;
    @Schema(description = "Các tệp đính kèm; kiểu phần tử theo schema của endpoint")
    private List<MediaItem> mediaUrls;
    @Schema(description = "Phạm vi hiển thị bài viết theo enum API")
    private PostVisibility visibility;
    @Schema(description = "Thông tin công khai của tác giả")
    private AuthorDto author;

    @Schema(description = "Tổng số lượt cảm xúc", example = "3")
    private int reactCount;
    @Schema(description = "Tổng số bình luận hiển thị theo thống kê của đối tượng", example = "3")
    private int commentCount;
    @Schema(description = "Người gọi đã thả cảm xúc vào đối tượng này", example = "true")
    private boolean reactedByMe;
    @Schema(description = "Cảm xúc hiện tại của người gọi; null nếu chưa thả cảm xúc")
    private ReactionType myReaction;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;
    @Schema(description = "Thời điểm cập nhật gần nhất, ISO-8601 UTC; có thể null nếu chưa ghi nhận", example = "2026-11-19T07:00:00Z")
    private Instant updatedAt;

    @Schema(description = "Ghi chú nhận xét của giảng viên; có thể null khi chưa có", example = "Cần bổ sung dẫn chứng và kiểm tra lại lời giải.")
    private String lecturerNote;
    @Schema(description = "Người gọi được phép chỉnh sửa ghi chú giảng viên trên bài này", example = "true")
    private boolean canEditLecturerNote;
    @Schema(description = "Thời điểm ghi nhận ghi chú giảng viên", example = "2026-11-19T07:00:00Z")
    private Instant lecturerNoteAddedAt;
    @Schema(description = "Giảng viên đã viết ghi chú, có thể null")
    private AuthorDto notedLecturer;

    @Schema(description = "ID môn học", example = "42")
    private Long subjectId;
    @Schema(description = "Tên môn học", example = "Cơ sở dữ liệu")
    private String subjectName;
    @Schema(description = "Mã môn học", example = "IT3290")
    private String subjectCode;

    @Schema(description = "ID phiên/bộ câu hỏi được liên kết, có thể null nếu không áp dụng", example = "42")
    private Long sessionId;
}
