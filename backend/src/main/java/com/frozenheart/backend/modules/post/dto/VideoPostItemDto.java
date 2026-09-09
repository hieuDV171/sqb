package com.frozenheart.backend.modules.post.dto;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPostItemDto {
    private Long postId;
    private String content;
    private PostVisibility visibility;
    private int reactCount;
    private int commentCount;
    private List<MediaItem> mediaUrls;
    private AuthorDto author;

    private Instant createdAt;
    private Instant updatedAt;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;
}
