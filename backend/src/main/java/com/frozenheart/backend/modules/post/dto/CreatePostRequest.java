package com.frozenheart.backend.modules.post.dto;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {
    private String content;
    private List<MediaItem> mediaUrls;

    @NotNull(message = "Visibility cannot be null")
    private PostVisibility visibility;

    private Long subjectId;
    private PostType postType;
}
