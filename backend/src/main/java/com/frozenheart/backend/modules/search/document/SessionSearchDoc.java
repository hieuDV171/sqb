package com.frozenheart.backend.modules.search.document;

import com.frozenheart.backend.core.entity.session.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSearchDoc {

    public static final String INDEX_NAME = "sqb_sessions";

    private Long id;
    private String title;
    private String topic;
    private String content; // Nội dung mô tả / thảo luận của phiên

    private Long subjectId;
    private String subjectName;

    // Trạng thái phiên (PENDING: chưa công khai, RESOLVED: công khai)
    private SessionStatus status;

    // Thông tin tác giả
    private Long authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String authorFrameUrl;

    // Chỉ số tương tác & số câu hỏi trong phiên
    private Integer questionCount;

    private Integer reactCount;
    private Integer commentCount;

    private LocalDateTime createdAt;
}
