package com.frozenheart.backend.modules.ai.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu trò chuyện tương tác với trợ lý AI")
public record AiChatRequest(
        @Schema(description = "Câu hỏi hoặc lời nhắc mới của người dùng", example = "Giải thích giúp tôi sự khác nhau giữa ArrayList và LinkedList")
        @NotBlank(message = "Prompt câu hỏi không được để trống. FE cần gửi câu hỏi mới của người dùng.")
        @Size(max = 1000, message = "Prompt câu hỏi không được vượt quá 1000 ký tự")
        String prompt,

        @Schema(description = "ID phiên trò chuyện để tiếp tục ngữ cảnh (để trống nếu tạo phiên mới)", example = "session_a1b2c3d4")
        String chatSessionId,

        @Schema(description = "Lịch sử 5-10 tin nhắn gần nhất do Frontend lưu giữ và gửi lên (Stateless context)")
        @Size(max = 10, message = "Lịch sử trò chuyện tối đa 10 tin nhắn gần nhất")
        List<@Valid ChatMessageDto> history,

        @Schema(description = "Ngữ cảnh môn học/chủ đề hiện tại")
        ChatContextDto context
) {
    @Schema(description = "Một tin nhắn trong lịch sử trò chuyện")
    public record ChatMessageDto(
            @Schema(description = "Vai trò người gửi: 'user' hoặc 'assistant'", example = "user")
            @NotBlank(message = "Role trong history không được để trống")
            @Pattern(regexp = "^(?i)(user|assistant)$", message = "Role trong history chỉ được phép là 'user' hoặc 'assistant'")
            String role,

            @Schema(description = "Nội dung tin nhắn trong lịch sử", example = "Chào bạn, hãy hỗ trợ tôi môn Cấu trúc dữ liệu")
            @NotBlank(message = "Content câu chat trong history không được để trống")
            @Size(max = 1000, message = "Nội dung tin nhắn trong history không được vượt quá 1000 ký tự")
            String content
    ) {}

    @Schema(description = "Thông tin ngữ cảnh học tập")
    public record ChatContextDto(
            @Schema(description = "Tên môn học đang học/ôn tập", example = "Cấu trúc dữ liệu và giải thuật")
            String subjectName,

            @Schema(description = "Chủ đề cụ thể", example = "Danh sách liên kết")
            String topic
    ) {}
}
