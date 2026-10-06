package com.frozenheart.backend.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Schema(description = "Kết quả tìm kiếm toàn cục đa thực thể")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalSearchResponseDto {

    @Schema(description = "Danh sách các kết quả tìm kiếm khớp")
    private List<SearchResultItemDto> items;

    @Schema(description = "Thông tin phân trang con trỏ (Cursor Pagination)")
    private CursorPaginationDto pagination;

    @Schema(description = "Tổng số lượng kết quả tìm thấy", example = "142")
    private Long totalHits;

    @Schema(description = "Chi tiết một phần tử kết quả tìm kiếm")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchResultItemDto {

        @Schema(description = "Loại thực thể tìm thấy (POST, USER, QUESTION, SESSION, SUBJECT)", example = "QUESTION")
        private SearchType type;

        @Schema(description = "Điểm số độ liên quan tìm kiếm (Elasticsearch score)", example = "3.45")
        private Double relevanceScore;

        @Schema(description = "Dữ liệu bài viết nếu type = POST")
        private PostSearchResultDto post;

        @Schema(description = "Dữ liệu người dùng nếu type = USER")
        private UserSearchResultDto user;

        @Schema(description = "Dữ liệu câu hỏi nếu type = QUESTION")
        private QuestionSearchResultDto question;

        @Schema(description = "Dữ liệu phiên đóng góp nếu type = SESSION")
        private SessionSearchResultDto session;

        @Schema(description = "Dữ liệu môn học nếu type = SUBJECT")
        private SubjectSearchResultDto subject;
    }

    @Schema(description = "Kết quả tìm kiếm bài viết")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PostSearchResultDto {
        @Schema(description = "ID bài viết", example = "101")
        private Long postId;

        @Schema(description = "Nội dung bài viết", example = "Mọi người cho mình hỏi bài tập này...")
        private String content;

        @Schema(description = "ID tác giả", example = "15")
        private Long authorId;

        @Schema(description = "Họ và tên tác giả", example = "Nguyễn Văn A")
        private String authorName;

        @Schema(description = "URL ảnh đại diện tác giả", example = "https://sqb.s3.../avatar.png")
        private String authorAvatarUrl;

        @Schema(description = "URL khung avatar của tác giả", example = "https://sqb.s3.../frame.png")
        private String authorFrameUrl;

        @Schema(description = "Loại bài viết", example = "QUESTION_SHARING")
        private PostType postType;

        @Schema(description = "Danh sách URL hình ảnh đính kèm bài viết")
        private List<String> mediaUrls;

        @Schema(description = "Số lượt tương tác cảm xúc", example = "12")
        private Integer reactCount;

        @Schema(description = "Số lượt bình luận", example = "5")
        private Integer commentCount;

        @Schema(description = "Thời điểm đăng bài", example = "2026-10-05T14:30:00Z")
        private Instant createdAt;
    }

    @Schema(description = "Kết quả tìm kiếm người dùng")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSearchResultDto {
        @Schema(description = "ID người dùng", example = "25")
        private Long userId;

        @Schema(description = "Họ và tên", example = "Trần Thị B")
        private String fullName;

        @Schema(description = "Mã số sinh viên hoặc mã giảng viên", example = "HE170001")
        private String studentLecturerCode;

        @Schema(description = "Email người dùng", example = "btt@fpt.edu.vn")
        private String email;

        @Schema(description = "Tiểu sử cá nhân", example = "Yêu thích thuật toán và Java")
        private String bio;

        @Schema(description = "URL ảnh đại diện", example = "https://sqb.s3.../avatar2.png")
        private String avatarUrl;

        @Schema(description = "URL khung avatar", example = "https://sqb.s3.../frame2.png")
        private String avatarFrameUrl;

        @Schema(description = "Trường / Viện đào tạo", example = "Trường ĐH FPT Hà Nội")
        private String schoolFaculty;

        @Schema(description = "Chuyên ngành đào tạo", example = "Kỹ thuật phần mềm")
        private String major;

        @Schema(description = "Vai trò người dùng trong hệ thống", example = "STUDENT")
        private UserRole role;

        @Schema(description = "Tổng số câu hỏi đã đề xuất", example = "18")
        private Integer totalProposedQuestions;

        @Schema(description = "Tổng số huy hiệu đã đạt được", example = "5")
        private Integer badgesCount;

        @Schema(description = "Số lượng người đang theo dõi", example = "42")
        private Integer followersCount;
    }

    @Schema(description = "Kết quả tìm kiếm câu hỏi trắc nghiệm")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionSearchResultDto {
        @Schema(description = "ID câu hỏi", example = "88")
        private Long questionId;

        @Schema(description = "Mã định danh câu hỏi", example = "PRN211-Q088")
        private String questionCode;

        @Schema(description = "ID môn học", example = "3")
        private Long subjectId;

        @Schema(description = "Tên môn học", example = "Lập trình C# cơ bản")
        private String subjectName;

        @Schema(description = "ID chủ đề", example = "10")
        private Long topicId;

        @Schema(description = "Tên chủ đề kiến thức", example = "LINQ to Objects")
        private String topicName;

        @Schema(description = "Trạng thái câu hỏi", example = "APPROVED")
        private QuestionStatus status;

        @Schema(description = "Độ khó câu hỏi", example = "MEDIUM")
        private QuestionDifficulty difficulty;

        @Schema(description = "Nội dung câu hỏi", example = "Trong LINQ, toán tử nào dùng để lọc phần tử theo điều kiện?")
        private String content;

        @Schema(description = "Danh sách các lựa chọn đáp án")
        private List<QuestionOptionDto> options;

        @Schema(description = "Giải thích chi tiết cho câu hỏi")
        private String explanation;

        @Schema(description = "Danh sách URL hình ảnh minh họa trong câu hỏi")
        private List<String> imageUrls;

        @Schema(description = "Điểm đánh giá trung bình từ cộng đồng [1.0 - 5.0]", example = "4.6")
        private Double avgRating;

        @Schema(description = "Số lượt đánh giá", example = "15")
        private Integer ratingCount;

        @Schema(description = "Số lượt thả biểu cảm", example = "8")
        private Integer reactCount;

        @Schema(description = "Số lượng bình luận thảo luận", example = "3")
        private Integer commentCount;

        @Schema(description = "Thời điểm tạo câu hỏi", example = "2026-10-04T10:00:00Z")
        private Instant createdAt;
    }

    @Schema(description = "Phương án trả lời của câu hỏi")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionOptionDto {
        @Schema(description = "Mã ký hiệu phương án (A, B, C, D)", example = "A")
        private String key;

        @Schema(description = "Nội dung văn bản của phương án", example = "Where")
        private String text;

        @Schema(description = "URL hình ảnh đính kèm của phương án (nếu có)", example = "null")
        private String mediaUrl;
    }

    @Schema(description = "Kết quả tìm kiếm phiên đóng góp câu hỏi")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SessionSearchResultDto {
        @Schema(description = "ID phiên đóng góp", example = "14")
        private Long sessionId;

        @Schema(description = "Tiêu đề phiên đóng góp", example = "Đóng góp câu hỏi ôn thi giữa kỳ PRN211")
        private String title;

        @Schema(description = "Mô tả chi tiết nội dung phiên", example = "Tập trung vào phần Entity Framework Core")
        private String content;

        @Schema(description = "ID môn học", example = "3")
        private Long subjectId;

        @Schema(description = "Tên môn học", example = "Lập trình C# cơ bản")
        private String subjectName;

        @Schema(description = "Trạng thái phiên", example = "ACTIVE")
        private SessionStatus status;

        @Schema(description = "Số lượng câu hỏi đã nộp vào phiên", example = "24")
        private Integer questionCount;

        @Schema(description = "Số lượt tương tác cảm xúc", example = "10")
        private Integer reactCount;

        @Schema(description = "Số bình luận", example = "4")
        private Integer commentCount;

        @Schema(description = "Thời gian tạo phiên", example = "2026-10-02T08:00:00Z")
        private Instant createdAt;
    }

    @Schema(description = "Kết quả tìm kiếm môn học")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubjectSearchResultDto {
        @Schema(description = "ID môn học", example = "3")
        private Long subjectId;

        @Schema(description = "Mã môn học", example = "PRN211")
        private String subjectCode;

        @Schema(description = "Tên môn học", example = "Lập trình C# cơ bản")
        private String subjectName;
    }
}
