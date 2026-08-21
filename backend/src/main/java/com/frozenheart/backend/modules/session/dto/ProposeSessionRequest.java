package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ProposeSessionRequest(
        @NotBlank(message = "Tiêu đề phiên không được để trống")
        String title,
                        
        String content,
        String sourceUrl,
        @NotNull(message = "ID môn học không được để trống")
        Long subjectId,
        @NotEmpty(message = "Danh sách câu hỏi không được để trống")
        @Valid
        List<QuestionProposeDto> questions
) {
    @Builder
    public record QuestionProposeDto(
            @NotBlank(message = "Nội dung câu hỏi không được để trống")
            String content,
            List<String> mediaUrls,
            @NotEmpty(message = "Danh sách đáp án không được để trống")
            List<QuestionOption> options,

            String explanation,
                    
            @NotNull(message = "Nguồn câu hỏi không được để trống")
            boolean llmGenerated,
            
            @Min(value = 0) @Max(value = 4)
            Double confidence
    ) {}
}
