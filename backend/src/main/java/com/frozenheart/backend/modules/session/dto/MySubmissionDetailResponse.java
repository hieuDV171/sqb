package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Chi tiết phiên đề xuất câu hỏi của người dùng")
public record MySubmissionDetailResponse(
        @Schema(description = "ID phiên đề xuất", example = "101")
        Long sessionId,

        @Schema(description = "Mã phiên nộp duy nhất", example = "IT3180_A7B2C1_1728200000000_101")
        String sessionCode,

        @Schema(description = "Tiêu đề phiên nộp", example = "Bộ câu hỏi ôn tập Chương 3")
        String title,

        @Schema(description = "Mô tả nội dung phiên", example = "5 câu trắc nghiệm Saga & CQRS")
        String content,

        @Schema(description = "ID môn học", example = "10")
        Long subjectId,

        @Schema(description = "Tên môn học", example = "Nhập môn Công nghệ phần mềm")
        String subjectName,

        @Schema(description = "Mã môn học", example = "IT3180")
        String subjectCode,

        @Schema(description = "Số lượng bình luận", example = "4")
        int commentCount,

        @Schema(description = "Số lượt thả cảm xúc", example = "12")
        int reactCount,

        @Schema(description = "Tên hoặc email giảng viên duyệt phiên (nếu đã được duyệt)", example = "TS. Nguyễn Văn B")
        String reviewedByLecturer,

        @Schema(description = "Thời điểm giảng viên hoàn tất duyệt phiên")
        Instant reviewedAt,

        @Schema(description = "Thời điểm tạo phiên")
        Instant createdAt,

        @Schema(description = "Danh sách câu hỏi trong phiên")
        List<MySubmissionQuestionDto> questions) {

    @Builder
    @Schema(description = "Chi tiết một câu hỏi trong phiên đề xuất")
    public record MySubmissionQuestionDto(
            @Schema(description = "ID câu hỏi", example = "1001")
            Long questionId,

            @Schema(description = "Nội dung câu hỏi", example = "Pattern nào sau đây giải quyết bài toán phân tán giao dịch?")
            String content,

            @Schema(description = "Danh sách URL ảnh đính kèm câu hỏi", example = "[\"https://minio.sqb.edu.vn/media/q1.png\"]")
            List<String> imageUrls,

            @Schema(description = "Danh sách các lựa chọn đáp án")
            List<QuestionOption> options,

            @Schema(description = "Lời giải thích chi tiết cho đáp án")
            String explanation,

            @Schema(description = "Nguồn gốc câu hỏi (HOMO_SAPIENS hoặc LLM)", example = "HOMO_SAPIENS")
            QuestionSource source,

            @Schema(description = "Mức độ tự tin của tác giả [0.0; 4.0]", example = "3.5")
            Double confidenceScore,

            @Schema(description = "Số bình luận", example = "2")
            int commentCount,

            @Schema(description = "Số lượt react", example = "8")
            int reactCount,

            @Schema(description = "Số lượt đánh giá chất lượng sao", example = "15")
            int ratingCount
        ) {
    }
}
