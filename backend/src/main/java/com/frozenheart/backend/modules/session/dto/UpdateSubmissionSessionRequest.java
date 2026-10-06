package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Schema(description = "Yêu cầu cập nhật phiên đề xuất câu hỏi")
@Builder
public record UpdateSubmissionSessionRequest(
        @Schema(description = "Tiêu đề phiên đề xuất", example = "Đề xuất trắc nghiệm Hệ điều hành Chương 3 (Đã sửa lỗi)")
        String title,

        @Schema(description = "Mô tả nội dung phiên đề xuất", example = "Bổ sung câu hỏi về giải thuật định thời CPU Round Robin")
        String content,

        @Schema(description = "Đường dẫn tài liệu nguồn tham khảo", example = "https://tailieu.hust.edu.vn/it3080/chapter3.pdf")
        String sourceUrl,

        @Schema(description = "ID môn học", example = "10")
        Long subjectId,

        @Schema(description = "Danh sách câu hỏi cần cập nhật hoặc thêm mới")
        @Valid List<QuestionUpdateDto> questions
) {
    @Schema(description = "Thông tin câu hỏi cập nhật trong phiên")
    @Builder
    public record QuestionUpdateDto(
            @Schema(description = "ID câu hỏi (để null nếu là câu hỏi mới thêm vào phiên)", example = "105")
            Long questionId,

            @Schema(description = "Nội dung câu hỏi (hỗ trợ LaTeX/Markdown)", example = "Thuật toán định thời nào sau đây có thể dẫn đến hiện tượng đói CPU (starvation)?")
            String content,

            @Schema(description = "Danh sách URL hình ảnh minh họa cho câu hỏi", example = "[\"https://sqb.s3.ap-southeast-1.amazonaws.com/questions/q1_chart.png\"]")
            List<String> mediaUrls,

            @Schema(description = "Danh sách các lựa chọn đáp án")
            List<QuestionOption> options,

            @Schema(description = "Lời giải chi tiết cho câu hỏi", example = "SJF ưu tiên tiến trình ngắn hơn, các tiến trình dài có thể bị đói CPU nếu tiến trình ngắn liên tục tới.")
            String explanation,

            @Schema(description = "Nguồn gốc tạo câu hỏi", example = "HOMO_SAPIENS")
            QuestionSource source,

            @Schema(description = "Mức độ tự tin của tác giả [0.0; 4.0] (0: sai hoàn toàn, 1: LLM bịa, 2: chưa vững, 3: cơ bản, 4: câu hỏi hay)", example = "3.5")
            @Min(value = 0) @Max(value = 4) Double confidence
    ) {}
}

