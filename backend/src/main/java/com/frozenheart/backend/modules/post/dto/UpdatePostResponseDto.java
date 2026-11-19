package com.frozenheart.backend.modules.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "ID bài viết và thời điểm cập nhật thành công")
public class UpdatePostResponseDto {
    @Schema(description = "ID bài viết", example = "42")
    private Long postId;

    @Schema(description = "Thời điểm cập nhật gần nhất, ISO-8601 UTC; có thể null nếu chưa ghi nhận", example = "2026-11-19T07:00:00Z")
    private Instant updatedAt;
}
