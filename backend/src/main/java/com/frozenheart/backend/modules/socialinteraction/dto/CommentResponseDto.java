package com.frozenheart.backend.modules.socialinteraction.dto;

import com.frozenheart.backend.modules.post.dto.AuthorDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private Long commentId;
    private String targetType;
    private Long targetId;
    private Long parentCommentId;

    private String content;
    private String mediaUrl;
    private AuthorDto author;

    private LocalDateTime createdAt;

    private boolean hidden;

    private int replyCount;
    private List<CommentResponseDto> replies;
}
