package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bộ câu hỏi có thể luyện tập sau khi chốt phiên, kèm phân trang con trỏ")
public record PracticeCatalogResponse(List<PracticeSetDto> contents, CursorPaginationDto pagination) {

    @Schema(description = "Thông tin bộ câu hỏi công khai cho người dùng đã đăng nhập")
    public record PracticeSetDto(Long sessionId, String sessionCode, String title, String content,
            String subjectName, String subjectCode, Integer questionCounts, Instant createdAt,
            Integer reactCount, ReactionType myReaction, Integer commentCount) {
    }
}
