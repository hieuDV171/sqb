package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Phản hồi danh sách câu hỏi trong phiên phục vụ luyện tập")
@Builder
public record SessionQuestionsResponse(
        @Schema(description = "ID phiên câu hỏi", example = "12")
        Long sessionId,
        @Schema(description = "Mã định danh phiên", example = "IT3080_A8F2C1_1728211000_12")
        String sessionCode,
        @Schema(description = "Tiêu đề phiên câu hỏi", example = "Ôn tập Hệ điều hành Chương 3")
        String title,
        @Schema(description = "Mô tả nội dung phiên", example = "Tuyển tập trắc nghiệm giải thuật định thời CPU")
        String content,
        @Schema(description = "ID môn học", example = "10")
        Long subjectId,
        @Schema(description = "Tên môn học", example = "Hệ điều hành")
        String subjectName,
        @Schema(description = "Mã học phần", example = "IT3080")
        String subjectCode,
        @Schema(description = "Thời gian tạo phiên", example = "2026-10-06T08:30:00Z")
        Instant createdAt,
        @Schema(description = "Tổng số câu hỏi trong phiên", example = "10")
        int totalQuestions,
        @Schema(description = "Tổng số lượt cảm xúc của bộ câu hỏi", example = "12")
        int reactCount,
        @Schema(description = "Cảm xúc của người gọi trên bộ câu hỏi, null nếu chưa thả cảm xúc", example = "LIKE")
        ReactionType myReaction,
        @Schema(description = "Danh sách câu hỏi luyện tập")
        List<PracticeQuestionDto> questions
    ) {
    @Schema(description = "Thông tin câu hỏi luyện tập")
    @Builder
    public record PracticeQuestionDto(
            @Schema(description = "ID câu hỏi", example = "105")
            Long questionId,
            @Schema(description = "Mã định danh câu hỏi", example = "IT3080_A8F2C1_105")
            String questionCode,
            @Schema(description = "Nội dung câu hỏi (hỗ trợ LaTeX/Markdown)", example = "Thuật toán định thời nào sau đây có thể dẫn đến hiện tượng đói CPU?")
            String content,
            @Schema(description = "Danh sách URL hình ảnh minh họa cho câu hỏi", example = "[\"https://media.example.com/question.png\"]")
            List<String> imageUrls,
            @Schema(description = "Danh sách các lựa chọn đáp án (đã ẩn trường is_correct nếu chưa trả lời)")
            List<QuestionOption> options,
            @Schema(description = "Nguồn gốc tạo câu hỏi", example = "HOMO_SAPIENS")
            QuestionSource source,
            @Schema(description = "Thời gian tạo", example = "2026-10-06T08:30:00Z")
            Instant createdAt,
            @Schema(description = "Thời gian cập nhật gần nhất", example = "2026-10-06T09:00:00Z")
            Instant updatedAt,
            @Schema(description = "Trạng thái tương tác của người dùng hiện tại đối với câu hỏi này")
            MyInteractionDto myInteraction,
            @Schema(description = "Các trường ẩn (chỉ hiển thị đáp án đúng và giải thích sau khi người dùng đã trả lời)")
            HiddenFieldsDto hiddenFields,
            @Schema(description = "Số lượt tương tác/cảm xúc", example = "8")
            int reactCount,
            @Schema(description = "Số lượng bình luận", example = "3")
            int commentCount,
            @Schema(description = "Số lượt đánh giá chất lượng", example = "12")
            int ratingCount
        ) {
    }

    @Schema(description = "Trạng thái tương tác của người dùng đối với câu hỏi")
    @Builder
    public record MyInteractionDto(
            @Schema(description = "Người dùng đã trả lời câu hỏi này chưa", example = "true")
            boolean answered,
            @Schema(description = "Người dùng đã đánh giá câu hỏi này chưa", example = "false")
            boolean rated,
            @Schema(description = "Cảm xúc hiện tại của người dùng, nếu đã tương tác", example = "LIKE")
            ReactionType myReaction,
            @Schema(description = "Điểm đánh giá chất lượng của người gọi; có thể null khi chưa đánh giá", example = "4.0")
            Double myRating,
            @Schema(description = "Nhận xét đi kèm đánh giá chất lượng", example = "Câu hỏi rõ ràng, có lời giải dễ hiểu.")
            String ratingComment,
            @Schema(description = "Người gọi đã báo cáo câu hỏi này hay chưa", example = "false")
            boolean reported
        ) {
    }

    @Schema(description = "Các trường chỉ tiết lộ sau khi trả lời")
    @Builder
    public record HiddenFieldsDto(
            @Schema(description = "Đáp án đúng", example = "A")
            String correctAnswer,
            @Schema(description = "Lời giải chi tiết câu hỏi", example = "SJF ưu tiên tiến trình ngắn, tiến trình dài có thể bị đói tài nguyên CPU.")
            String explanation,
            @Schema(description = "Các lựa chọn người dùng đã nộp, gồm nội dung tại thời điểm trả lời")
            List<SubmittedOptionDto> selectedOptions,
            @Schema(description = "Lần trả lời đã nộp đúng hay sai; có thể null nếu chưa có kết quả", example = "true")
            Boolean wasCorrect,
            @Schema(description = "Câu hỏi đã thay đổi kể từ lần trả lời đang hiển thị", example = "false")
            boolean questionUpdatedSinceAnswer
        ) {
    }

    @Schema(description = "Một lựa chọn đã nộp, lưu key và nội dung để so sánh khi câu hỏi được sửa")
    public record SubmittedOptionDto(
            @Schema(description = "Khóa phương án đã nộp, ví dụ A/B/C/D", example = "A")
            String key,
            @Schema(description = "Nội dung phương án tại thời điểm người dùng trả lời", example = "Lựa chọn thứ nhất")
            String text) {
    }
}

