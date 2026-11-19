package com.frozenheart.backend.core.entity.socialinteraction;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Bản chụp nội dung/nguồn tác giả tại một thời điểm phục vụ đối chiếu báo cáo")
public record ReportSnapshot(
        @Schema(description = "ID tác giả nội dung tại thời điểm chụp snapshot", example = "42")
        Long authorId,
        @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
        String content,
        @Schema(description = "Thời điểm cập nhật gần nhất, ISO-8601 UTC; có thể null nếu chưa ghi nhận", example = "2026-11-19T07:00:00Z")
        Instant updatedAt) {}
