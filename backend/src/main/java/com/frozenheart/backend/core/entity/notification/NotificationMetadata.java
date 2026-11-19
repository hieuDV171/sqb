package com.frozenheart.backend.core.entity.notification;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata thông báo; các trường được dùng tùy notification type")
public class NotificationMetadata {

    @Schema(description = "Các câu hỏi được xác nhận kèm thông tin cộng điểm trong thông báo tương ứng")
    private List<ConfirmedQuestionDto> confirmedQuestions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Một câu hỏi được duyệt và phần thưởng được thông báo")
    public static class ConfirmedQuestionDto {
        @Schema(description = "ID câu hỏi được duyệt", example = "42")
        private Long questionId;
        @Schema(description = "Tên môn học của câu hỏi", example = "Cơ sở dữ liệu")
        private String subjectName;
        @Schema(description = "Trích đoạn nội dung câu hỏi", example = "Khóa chính của một bảng có tính chất nào?")
        private String questionSnippet;
        @Schema(description = "Số điểm thưởng cho câu hỏi này", example = "2.5")
        private double points;
    }
}
