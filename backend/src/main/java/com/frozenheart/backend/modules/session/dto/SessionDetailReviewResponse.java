package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.QuestionOption;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Thông tin chi tiết phiên đề xuất dành cho giảng viên duyệt")
@Builder
public record SessionDetailReviewResponse(
        @Schema(description = "Danh sách câu hỏi cần duyệt trong phiên")
        List<SessionQuestionReviewDto> items,

        @Schema(description = "Danh sách cảnh báo trùng lặp được phát hiện tự động bởi hệ thống")
        List<DuplicateWarning> duplicateWarnings) {

        @Schema(description = "Chi tiết câu hỏi để duyệt")
        @Builder
        public record SessionQuestionReviewDto(
                @Schema(description = "ID câu hỏi", example = "105")
                Long questionId,

                @Schema(description = "Nội dung câu hỏi (hỗ trợ LaTeX/Markdown)", example = "Thuật toán định thời nào sau đây có thể dẫn đến hiện tượng đói CPU (starvation)?")
                String content,

                @Schema(description = "Danh sách URL ảnh minh họa nội dung câu hỏi", example = "[\"https://sqb.s3.ap-southeast-1.amazonaws.com/questions/q1_chart.png\"]")
                List<String> imageUrls,

                @Schema(description = "Danh sách các lựa chọn đáp án")
                List<QuestionOption> options,

                @Schema(description = "Khóa đáp án đúng", example = "B")
                String correctAnswer,

                @Schema(description = "Lời giải thích đáp án", example = "Thuật toán SJF ưu tiên tiến trình có burst time ngắn nhất...")
                String explanation,

                @Schema(description = "Trạng thái câu hỏi (PENDING, APPROVED, REJECTED)", example = "PENDING")
                String status,

                @Schema(description = "Nhật ký chỉnh sửa gần nhất của câu hỏi (nếu có)")
                QuestionEditLogDto editLog) {
        }

        @Schema(description = "Nhật ký chỉnh sửa câu hỏi")
        @Builder
        public record QuestionEditLogDto(
                @Schema(description = "ID nhật ký chỉnh sửa", example = "201")
                Long editLogId,

                @Schema(description = "Loại đối tượng thực hiện chỉnh sửa (SYSTEM, LECTURER)", example = "LECTURER")
                String actorType,

                @Schema(description = "Tên hiển thị của người/hệ thống chỉnh sửa", example = "TS. Nguyễn Văn A")
                String actorName,

                @Schema(description = "Trạng thái nhật ký", example = "PROPOSED")
                String status,

                @Schema(description = "Dữ liệu trước khi chỉnh sửa")
                Object beforeState,

                @Schema(description = "Dữ liệu sau khi chỉnh sửa")
                Object afterState,

                @Schema(description = "Kết quả kiểm định ảo giác AI (Hallucination Audit)")
                Object hallucinationAudit,

                @Schema(description = "Thời gian ghi nhận chỉnh sửa", example = "2026-10-06T09:15:00Z")
                Instant createdAt) {
        }
}

