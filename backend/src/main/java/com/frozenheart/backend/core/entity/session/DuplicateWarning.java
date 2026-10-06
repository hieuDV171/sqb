package com.frozenheart.backend.core.entity.session;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Cảnh báo trùng lặp câu hỏi được phát hiện bởi hệ thống")
@Builder
public record DuplicateWarning(
        @Schema(description = "Thứ tự của câu hỏi trong phiên nộp", example = "0")
        int questionIndex,

        @Schema(description = "ID của câu hỏi tương đồng đã tồn tại trong ngân hàng đề", example = "1024")
        Long similarQuestionId,

        @Schema(description = "Điểm số tương đồng (0.0 - 1.0)", example = "0.92")
        double similarityScore,

        @Schema(description = "Tầng phát hiện trùng lặp", example = "RULE_BASED")
        DuplicateDetectionTier tier,

        @Schema(description = "Trích đoạn nội dung bị trùng lặp", example = "Thuật toán Round Robin phân chia thời gian CPU như thế nào...")
        String matchedSnipet,

        @Schema(description = "URL hình ảnh bị trùng lặp (nếu phát hiện trùng qua ảnh)", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/questions/duplicate.png")
        String mediaUrl
    ) {

}

