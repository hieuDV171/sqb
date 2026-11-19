package com.frozenheart.backend.modules.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "ID bài viết vừa được xóa theo quyền của người gọi")
public class DeletePostResponseDto {
    @Schema(description = "ID bài viết", example = "42")
    private Long postId;
}
