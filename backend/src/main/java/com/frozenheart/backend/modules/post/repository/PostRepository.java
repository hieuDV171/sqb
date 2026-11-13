package com.frozenheart.backend.modules.post.repository;

import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    String LECTURE_VIDEO_ACCESS = """
            (:admin = true OR p.poster.id = :viewerId OR EXISTS (
                SELECT 1 FROM UserCourseClass enrollment JOIN enrollment.courseClass cc
                WHERE enrollment.id.userId = :viewerId AND cc.subject.id = p.subject.id
            ))
            """;

    String POST_VISIBILITY_ACCESS = """
            (:admin = true OR p.poster IS NULL OR p.poster.id = :viewerId OR (
                NOT EXISTS (
                    SELECT 1 FROM UserBlock b
                    WHERE (b.blocker.id = :viewerId AND b.blocked.id = p.poster.id)
                       OR (b.blocker.id = p.poster.id AND b.blocked.id = :viewerId)
                )
                AND (p.visibility IS NULL OR p.visibility = 'PUBLIC' OR (
                    p.visibility = 'FRIENDS' AND EXISTS (
                        SELECT 1 FROM Friendship f
                        WHERE f.status = 'ACCEPTED'
                          AND ((f.sender.id = :viewerId AND f.receiver.id = p.poster.id)
                            OR (f.sender.id = p.poster.id AND f.receiver.id = :viewerId))
                    )
                ))
            ))
            """;

    @Modifying
    @Query("UPDATE Post p SET p.reactCount = p.reactCount + :delta WHERE p.id = :postId")
    void incrementReactCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session.proposer"})
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session.proposer"})
    List<Post> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @Query("""
            SELECT p FROM Post p JOIN FETCH p.session s
            LEFT JOIN FETCH s.proposer
            LEFT JOIN FETCH p.notedLecturer
            WHERE s.id IN :sessionIds AND p.deletedAt IS NULL AND p.postType = 'QUESTION_APPROVED_NOTIFICATION'
              AND p.visibility = 'PUBLIC' AND s.status = 'RESOLVED'
            """)
    List<Post> findPublicApprovalPostsBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    @Query("""
            SELECT p.id FROM Post p JOIN p.session s JOIN s.courseClass cc JOIN s.proposer student
            WHERE p.id IN :postIds AND p.deletedAt IS NULL AND p.postType = 'QUESTION_APPROVED_NOTIFICATION'
              AND s.status = 'RESOLVED' AND student.role = 'STUDENT'
              AND cc.lecturer.id = :lecturerId
            """)
    List<Long> findApprovalPostIdsForLecturer(@Param("postIds") Collection<Long> postIds,
                                            @Param("lecturerId") Long lecturerId);

    @Query("SELECT p FROM Post p JOIN FETCH p.poster u WHERE p.deletedAt IS NULL AND p.visibility != com.frozenheart.backend.core.entity.post.PostVisibility.ONLY_ME")
    List<Post> findAllPublicPostsForSearch();

    interface SearchPostCounters {
        Long getPostId();
        int getReactCount();
        int getCommentCount();
    }

    @Query("""
            SELECT p.id AS postId, p.reactCount AS reactCount, p.commentCount AS commentCount
            FROM Post p
            WHERE p.id IN :ids AND p.deletedAt IS NULL
              AND (p.postType IS NULL OR p.postType <> 'LEARNING_VIDEO' OR
            """ + LECTURE_VIDEO_ACCESS + """
              )
              AND
            """ + POST_VISIBILITY_ACCESS)
    List<SearchPostCounters> findSearchPostCounters(@Param("ids") Collection<Long> ids,
                                                     @Param("viewerId") Long viewerId,
                                                     @Param("admin") boolean admin);

    @Query("SELECT p FROM Post p JOIN FETCH p.poster u WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findByIdFetchPosterForSearch(@Param("id") Long id);

    interface PostInteractionTarget {
        Long getId();
        PostType getPostType();
    }

    @Query("SELECT p.id AS id, p.postType AS postType FROM Post p WHERE p.id = :postId")
    Optional<PostInteractionTarget> findInteractionTarget(@Param("postId") Long postId);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Post p
            WHERE p.id = :postId AND p.deletedAt IS NULL AND p.postType = 'LEARNING_VIDEO'
              AND
            """ + LECTURE_VIDEO_ACCESS + "AND " + POST_VISIBILITY_ACCESS)
    boolean canViewLectureVideo(@Param("postId") Long postId, @Param("viewerId") Long viewerId,
                                @Param("admin") boolean admin);

    @Query("""
            SELECT p FROM Post p LEFT JOIN FETCH p.poster LEFT JOIN FETCH p.subject
            WHERE p.postType = 'LEARNING_VIDEO' AND p.deletedAt IS NULL AND p.id < :after
              AND (:subjectId IS NULL OR p.subject.id = :subjectId) AND
            """ + LECTURE_VIDEO_ACCESS + "AND " + POST_VISIBILITY_ACCESS + "ORDER BY p.id DESC")
    List<Post> findVisibleLectureVideos(@Param("viewerId") Long viewerId, @Param("admin") boolean admin,
                                        @Param("subjectId") Long subjectId, @Param("after") Long after,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session.proposer"})
    List<Post> findByPosterIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            Long posterId,
            Long after,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session.proposer"})
    @Query("""
            SELECT p FROM Post p
            WHERE p.poster.id = :posterId AND p.id < :after AND p.deletedAt IS NULL
              AND p.visibility IN :visibilities
              AND (p.postType IS NULL OR p.postType <> 'LEARNING_VIDEO' OR
            """ + LECTURE_VIDEO_ACCESS + """
              )
            ORDER BY p.id DESC
            """)
    List<Post> findVisibleProfilePosts(
            @Param("posterId") Long posterId,
            @Param("viewerId") Long viewerId,
            @Param("admin") boolean admin,
            @Param("visibilities") Collection<PostVisibility> visibilities,
            @Param("after") Long after,
            Pageable pageable
    );
}
