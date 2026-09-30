package com.frozenheart.backend.modules.post.dto;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostType;
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
public class PostResponseDto {
    private Long postId;
    private String content;
    private PostType postType;
    private List<MediaItem> mediaUrls;
    private PostVisibility visibility;
    private AuthorDto author;

    private int reactCount;
    private int commentCount;
    private boolean reactedByMe;

    private Instant createdAt;
    private Instant updatedAt;

    private String lecturerNote;
    private Instant lecturerNoteAddedAt;
    private AuthorDto notedLecturer;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private Long sessionId;
}
