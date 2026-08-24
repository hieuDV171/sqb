package com.frozenheart.backend.core.entity.session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionOption {
    private String key;
    private String text;
    private Boolean isCorrect;

    // Tại sao ở đây lưu mediaUrl mà QuestionMedia cũng lưu url ảnh của câu hỏi luôn
    // 1. Lưu vào QuestionMedia để xử lý tập trung, đưa cho AI xử lý ảnh chỉ cần truy vấn vào bảng QuestionMedia
    // 2. Lưu thêm ở đây vì thay vì phải JOIN rồi kiểm tra ảnh nào của câu hỏi, ảnh nào của đáp án, rồi ảnh nào của đáp án nào, ta chi cần lấy trực tiếp url này

    private String mediaUrl;
    private Long mediaId;
}
