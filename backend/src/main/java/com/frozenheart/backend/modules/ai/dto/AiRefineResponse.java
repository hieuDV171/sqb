package com.frozenheart.backend.modules.ai.dto;

import java.util.List;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả AI tinh chỉnh câu hỏi kèm kiểm định ảo giác Btprop")
@Builder
public record AiRefineResponse(
        @Schema(description = "ID nhật ký chỉnh sửa đã ghi nhận", example = "105")
        Long editLogId,

        @Schema(description = "Nội dung câu hỏi được AI đề xuất mới")
        SuggestedQuestionDto suggestedQuestion,

        @Schema(description = "Kết quả kiểm định ảo giác từ dịch vụ Btprop")
        BtpropAuditDto hallucinationAudit,

        @Schema(description = "Thông số xử lý của mô hình AI")
        AiMetadataDto aiMetadata
) {
    @Schema(description = "Chi tiết câu hỏi do AI đề xuất")
    @Builder
    public record SuggestedQuestionDto(
            @Schema(description = "Nội dung đề bài mới", example = "Trong Java, từ khóa nào dùng để ngăn chặn việc kế thừa class?")
            String content,

            @Schema(description = "Danh sách các phương án trả lời mới")
            List<QuestionOption> options,

            @Schema(description = "Giải thích chi tiết cho đáp án đúng", example = "Từ khóa final dùng trước class để ngăn chặn việc kế thừa.")
            String explanation,

            @Schema(description = "Lý do từ chối nếu yêu cầu vi phạm chính sách", example = "null")
            String refusalReason
    ) {}

    @Schema(description = "Kết quả đánh giá độ tin cậy và ảo giác (Hallucination Audit)")
    @Builder
    public record BtpropAuditDto(
            @Schema(description = "Có phát hiện ảo giác/thông tin sai lệch hay không", example = "false")
            boolean isHallucinated,

            @Schema(description = "Điểm tin cậy của câu hỏi do AI đề xuất [0.0 - 1.0]", example = "0.95")
            double confidenceScore,

            @Schema(description = "Độ sâu của cây niềm tin kiểm định", example = "3")
            int beliefTreeDepth,

            @Schema(description = "Danh sách các vi phạm logic nếu có")
            List<ViolationDto> violations
    ) {}

    @Schema(description = "Thông tin vi phạm logic được phát hiện")
    @Builder
    public record ViolationDto(
            @Schema(description = "Loại vi phạm", example = "CONTRADICTION")
            String type,

            @Schema(description = "Mệnh đề logic trong cây niềm tin", example = "Node 2: class final can be extended")
            String nodeStatement,

            @Schema(description = "Chi tiết vi phạm", example = "Mâu thuẫn với định lý cấu trúc OOP Java")
            String detail,

            @Schema(description = "Mức độ nghiêm trọng (LOW, MEDIUM, HIGH, CRITICAL)", example = "HIGH")
            String severity
    ) {}
}
