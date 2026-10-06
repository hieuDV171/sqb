package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin phiên minigame Game 6 đang mở (Thứ 7 hàng tuần)")
@Builder
public record Game6ActiveSessionResponse(
        @Schema(description = "ID phiên minigame", example = "3")
        Long sessionId,

        @Schema(description = "Số thứ tự tuần trong năm", example = "40")
        int weekNumber,

        @Schema(description = "Năm diễn ra minigame", example = "2026")
        int year,

        @Schema(description = "Thời điểm bắt đầu phiên", example = "2026-10-10T00:00:00Z")
        Instant startTime,

        @Schema(description = "Thời điểm kết thúc phiên", example = "2026-10-10T23:59:59Z")
        Instant endTime,

        @Schema(description = "Danh sách câu hỏi thử thách (gồm cả do AI và con người tạo)")
        List<Game6QuestionDto> questions) {

    @Schema(description = "Câu hỏi trong thử thách Game 6")
    @Builder
    public record Game6QuestionDto(
            @Schema(description = "ID câu hỏi", example = "105")
            Long questionId,

            @Schema(description = "Nội dung câu hỏi (hỗ trợ LaTeX/Markdown)", example = "Thuật toán định thời nào sau đây có thể dẫn đến hiện tượng đói CPU?")
            String content,

            @Schema(description = "Danh sách các lựa chọn đáp án")
            List<QuestionOption> options,

            @Schema(description = "Lời giải chi tiết câu hỏi")
            String explanation,

            @Schema(description = "Danh sách URL ảnh đính kèm (nếu có)")
            List<String> imageUrls) {
    }
}

