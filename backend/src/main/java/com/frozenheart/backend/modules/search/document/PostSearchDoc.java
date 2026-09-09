package com.frozenheart.backend.modules.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchDoc {

    public static final String INDEX_NAME = "sqb_posts";

    private Long id;
    private String content;

    // Tác giả bài viết (Denormalized)
    private Long authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String authorFrameUrl;

    // Phân loại & Quyền riêng tư
    private PostType postType; // LEARNING_VIDEO, SOCIAL_POST, QUESTION_APPROVED_NOTIFICATION
    private PostVisibility visibility; // PUBLIC, FRIENDS, ONLY_ME

    // Danh sách URL ảnh/video đính kèm (để hiển thị thumbnail trên kết quả tìm
    // kiếm)
    private List<String> mediaUrls;

    // Chỉ số tương tác xếp hạng
    private Integer reactCount;
    private Integer commentCount;

    private Instant createdAt;
}
