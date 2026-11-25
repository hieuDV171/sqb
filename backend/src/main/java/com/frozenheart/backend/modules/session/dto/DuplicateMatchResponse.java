package com.frozenheart.backend.modules.session.dto;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Chi tiết của một câu đối sánh trong cảnh báo trùng khi duyệt")
public record DuplicateMatchResponse(
        @Schema(description = "ID câu đối sánh", example = "208") Long questionId,
        @Schema(description = "Nội dung câu đối sánh", example = "Giao thức HTTP dùng để làm gì?") String content,
        @Schema(description = "Đường dẫn ảnh của đề bài", example = "[\"https://media.example.com/question.png\"]") List<String> imageUrls,
        @Schema(description = "Các phương án trả lời, gồm nội dung, ảnh và cờ đúng") List<QuestionOption> options,
        @Schema(description = "Lời giải của câu đối sánh", example = "HTTP truyền tải tài nguyên trên Web.") String explanation) {}
