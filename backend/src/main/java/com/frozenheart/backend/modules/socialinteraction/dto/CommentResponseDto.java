package com.frozenheart.backend.modules.socialinteraction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.modules.post.dto.AuthorDto;
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
@Schema(description = "Bình luận hoặc phản hồi, tác giả và dữ liệu cây phản hồi hiển thị cho người gọi")
public class CommentResponseDto {
    @Schema(description = "ID bình luận hoặc phản hồi", example = "42")
    private Long commentId;
    @Schema(description = "Loại đối tượng được tham chiếu; dùng giá trị enum API công bố", example = "POST")
    private String targetType;
    @Schema(description = "ID đối tượng được tham chiếu; diễn giải cùng target_type/type của API", example = "42")
    private Long targetId;
    @Schema(description = "ID bình luận cha; null với bình luận gốc", example = "42")
    private Long parentCommentId;

    @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
    private String content;
    @Schema(description = "URL tệp/ảnh đính kèm, có thể null khi không có", example = "https://media.example.com/image.png")
    private String mediaUrl;
    @Schema(description = "Thông tin công khai của tác giả")
    private AuthorDto author;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;
    @Schema(description = "Bình luận đã xóa, client hiển thị placeholder thay nội dung gốc nếu API giữ nút cây", example = "false")
    private boolean deleted;

    @Schema(description = "Tổng số phản hồi theo thống kê của bình luận", example = "3")
    private int replyCount;
    @Schema(description = "Các phản hồi có sẵn trong kết quả; tải tiếp theo API replies khi cần")
    private List<CommentResponseDto> replies;
}
