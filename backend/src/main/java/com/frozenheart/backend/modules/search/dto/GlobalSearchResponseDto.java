package com.frozenheart.backend.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.UserRole;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalSearchResponseDto {

    private List<SearchResultItemDto> items;
    private CursorPaginationDto pagination;
    private Long totalHits;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchResultItemDto {
        private SearchType type;
        private Double relevanceScore;

        private PostSearchResultDto post;
        private UserSearchResultDto user;
        private QuestionSearchResultDto question;
        private SessionSearchResultDto session;
        private SubjectSearchResultDto subject;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PostSearchResultDto {
        private Long postId;
        private String content;
        private Long authorId;
        private String authorName;
        private String authorAvatarUrl;
        private String authorFrameUrl;
        private PostType postType;
        private List<String> mediaUrls;
        private Integer reactCount;
        private Integer commentCount;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSearchResultDto {
        private Long userId;
        private String fullName;
        private String studentLecturerCode;
        private String email;
        private String bio;
        private String avatarUrl;
        private String avatarFrameUrl;
        private String schoolFaculty;
        private String major;
        private UserRole role;
        private Integer totalProposedQuestions;
        private Integer badgesCount;
        private Integer followersCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionSearchResultDto {
        private Long questionId;
        private String questionCode;
        private Long subjectId;
        private String subjectName;
        private Long topicId;
        private String topicName;
        private QuestionStatus status;
        private QuestionDifficulty difficulty;

        // Nội dung câu hỏi tìm kiếm
        private String content;
        private List<QuestionOptionDto> options;
        private String explanation;

        private List<String> imageUrls;
        private Double avgRating;
        private Integer ratingCount;
        private Integer reactCount;
        private Integer commentCount;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionOptionDto {
        private String key;
        private String text;
        private String mediaUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SessionSearchResultDto {
        private Long sessionId;
        private String title;
        private String content;
        private Long subjectId;
        private String subjectName;
        private SessionStatus status;
        private Integer questionCount;
        private Integer reactCount;
        private Integer commentCount;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubjectSearchResultDto {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
    }
}
