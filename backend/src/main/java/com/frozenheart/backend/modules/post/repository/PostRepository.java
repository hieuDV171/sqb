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

    @Modifying
    @Query("UPDATE Post p SET p.reactCount = p.reactCount + :delta WHERE p.id = :postId")
    void incrementReactCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session"})
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session"})
    List<Post> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @Query("""
            SELECT p FROM Post p JOIN FETCH p.session s
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
              AND (:admin = true OR p.poster.id = :viewerId OR (
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
            """)
    List<SearchPostCounters> findSearchPostCounters(@Param("ids") Collection<Long> ids,
                                                     @Param("viewerId") Long viewerId,
                                                     @Param("admin") boolean admin);

    @Query("SELECT p FROM Post p JOIN FETCH p.poster u WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findByIdFetchPosterForSearch(@Param("id") Long id);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer"})
    List<Post> findByPostTypeAndSubjectIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            PostType postType,
            Long subjectId,
            Long after,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer"})
    List<Post> findByPostTypeAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            PostType postType,
            Long after,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session"})
    List<Post> findByPosterIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            Long posterId,
            Long after,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session"})
    List<Post> findByPosterIdAndVisibilityInAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            Long posterId,
            Collection<PostVisibility> visibilities,
            Long after,
            Pageable pageable
    );
}
