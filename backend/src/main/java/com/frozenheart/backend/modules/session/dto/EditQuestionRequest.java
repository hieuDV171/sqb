package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Yêu cầu giảng viên chỉnh sửa nội dung câu hỏi")
@Builder
public record EditQuestionRequest(
        @Schema(description = "Nội dung câu hỏi được biên tập lại", example = "Thuật toán định thời CPU nào sau đây có thể gây hiện tượng đói tiến trình?")
        String content,

        @Schema(description = "Danh sách đáp án đã được chỉnh sửa")
        List<QuestionOption> options,

        @Schema(description = "Lời giải chi tiết sau khi chỉnh sửa", example = "SJF ưu tiên tiến trình ngắn, tiến trình dài có thể bị đói tài nguyên CPU.")
        String explanation,

        @Schema(description = "Tự động duyệt câu hỏi vào ngân hàng đề sau khi sửa (true: duyệt ngay, false: giữ trạng thái chờ)", example = "true")
        Boolean autoApprove
) {}

