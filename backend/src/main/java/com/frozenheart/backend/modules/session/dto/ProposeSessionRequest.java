package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "Yêu cầu đề xuất phiên nộp bộ câu hỏi trắc nghiệm mới")
public record ProposeSessionRequest(
        @Schema(description = "Tiêu đề của phiên đề xuất", example = "Bộ câu hỏi ôn tập Chương 3 - Thiết kế phần mềm kiến trúc Microservices", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Tiêu đề phiên không được để trống")
        String title,
                        
        @Schema(description = "Mô tả tổng quan hoặc ghi chú thêm cho phiên", example = "Bao gồm 5 câu hỏi phân loại Design Patterns và Microservice patterns")
        String content,

        @Schema(description = "Nguồn tài liệu tham khảo (URL giáo trình, tài liệu tham khảo online)", example = "https://microservices.io/patterns")
        String sourceUrl,

        @Schema(description = "ID môn học trong hệ thống mà sinh viên đang theo học", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "ID môn học không được để trống")
        Long subjectId,

        @Schema(description = "Danh sách các câu hỏi đề xuất trong phiên", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        @Valid
        List<QuestionProposeDto> questions
) {
    @Builder
    @Schema(description = "Thông tin chi tiết một câu hỏi đề xuất")
    public record QuestionProposeDto(
            @Schema(description = "Nội dung câu hỏi", example = "Pattern nào sau đây giải quyết bài toán phân tán giao dịch trong kiến trúc Microservices?", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Nội dung câu hỏi không được để trống")
            String content,

            @Schema(description = "Danh sách URL ảnh đính kèm minh họa đề bài", example = "[\"https://minio.sqb.edu.vn/media/questions/q1_diagram.png\"]")
            List<String> mediaUrls,

            @Schema(description = "Danh sách các lựa chọn đáp án", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotEmpty(message = "Danh sách đáp án không được để trống")
            List<QuestionOption> options,

            @Schema(description = "Lời giải thích chi tiết cho đáp án đúng", example = "Saga Pattern chia transaction phân tán thành chuỗi các local transaction với các compensating transaction để rollback.")
            String explanation,
                    
            @Schema(description = "Đánh dấu câu hỏi do AI/LLM sinh (true) hay do tác giả tự biên soạn (false)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "Nguồn câu hỏi không được để trống")
            boolean llmGenerated,
            
            @Schema(description = "Mức độ tự tin của tác giả [0.0; 4.0]: 0-Quá tệ/sai hoàn toàn, 1-LLM bịa ra, 2-Chưa nắm vững, 3-Cơ bản, 4-Câu hỏi hay", example = "3.5")
            @Min(value = 0) @Max(value = 4)
            Double confidence
    ) {}
}
