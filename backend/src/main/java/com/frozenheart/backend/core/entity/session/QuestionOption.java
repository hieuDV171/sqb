package com.frozenheart.backend.core.entity.session;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Lựa chọn đáp án của câu hỏi trắc nghiệm")
public class QuestionOption {
    @Schema(description = "Ký tự định danh đáp án (A, B, C, D...)", example = "A")
    private String key;

    @Schema(description = "Nội dung văn bản của đáp án", example = "Mô hình Thác nước (Waterfall)")
    private String text;

    @Schema(description = "Cờ đánh dấu đáp án đúng", example = "true")
    private Boolean isCorrect;

    // Tại sao ở đây lưu mediaUrl mà QuestionMedia cũng lưu url ảnh của câu hỏi luôn
    // 1. Lưu vào QuestionMedia để xử lý tập trung, đưa cho AI xử lý ảnh chỉ cần truy vấn vào bảng QuestionMedia
    // 2. Lưu thêm ở đây vì thay vì phải JOIN rồi kiểm tra ảnh nào của câu hỏi, ảnh nào của đáp án, rồi ảnh nào của đáp án nào, ta chi cần lấy trực tiếp url này

    @Schema(description = "URL ảnh minh họa kèm theo đáp án (nếu có)", example = "https://minio.sqb.edu.vn/media/options/waterfall.png")
    private String mediaUrl;

    @Schema(description = "ID media đính kèm (nếu có)", example = "105")
    private Long mediaId;
}
