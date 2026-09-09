package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record AnswerQuestionRequest(
                @NotEmpty(message = "Danh sách đáp án chọn không được để trống") List<String> selectedOptions,

                // Dùng để kiểm tra câu hỏi đã cũ chưa, đề phòng trường hợp
                // FE bằng cách nào đó cho phép tác giả sửa câu hỏi trong lúc làm bài
                Instant questionUpdatedAt) {
}
