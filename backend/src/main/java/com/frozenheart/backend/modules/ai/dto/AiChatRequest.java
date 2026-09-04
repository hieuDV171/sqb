package com.frozenheart.backend.modules.ai.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO Yêu cầu Chat với AI.
 * 💡 LƯU Ý DÀNH CHO FRONTEND (FE):
 * Mô hình AI Server là Stateless (không lưu bộ nhớ giữa các lần gọi).
 * FE bắt buộc phải tự lưu giữ Lịch sử trò chuyện (State/LocalStorage)
 * và gửi danh sách 5-10 câu chat gần nhất trong mảng 'history' mỗi khi user bấm gửi tin nhắn mới!
 */
public record AiChatRequest(
        @NotBlank(message = "Prompt câu hỏi không được để trống. FE cần gửi câu hỏi mới của người dùng.")
        @Size(max = 1000, message = "Prompt câu hỏi không được vượt quá 1000 ký tự")
        String prompt,

        String chatSessionId,

        /*
         * Lịch sử hội thoại do FE tự quản lý và truyền lên.
         * Bắt buộc từng phần tử chỉ có role là 'user' hoặc 'assistant'.
         */
        @Size(max = 10, message = "Lịch sử trò chuyện tối đa 10 tin nhắn gần nhất")
        List<@Valid ChatMessageDto> history,

        ChatContextDto context
) {
    public record ChatMessageDto(
            @NotBlank(message = "Role trong history không được để trống")
            @Pattern(regexp = "^(?i)(user|assistant)$", message = "Role trong history chỉ được phép là 'user' hoặc 'assistant'")
            String role,

            @NotBlank(message = "Content câu chat trong history không được để trống")
            @Size(max = 1000, message = "Nội dung tin nhắn trong history không được vượt quá 1000 ký tự")
            String content
    ) {}

    public record ChatContextDto(
            String subjectName,
            String topic
    ) {}
}
