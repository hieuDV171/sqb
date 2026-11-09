package com.frozenheart.backend.modules.socialinteraction.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "parentComment"})
    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByTargetTypeAndTargetIdAndParentCommentIsNullAndIdLessThanOrderByIdDesc(
            String targetType, Long targetId, Long after, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByTargetTypeAndTargetIdAndParentCommentIsNullAndIdGreaterThanOrderByIdAsc(
            String targetType, Long targetId, Long after, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByParentCommentIdAndTargetTypeAndTargetIdAndIdLessThanOrderByIdDesc(
            Long parentCommentId, String targetType, Long targetId, Long after, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByParentCommentIdAndTargetTypeAndTargetIdAndIdGreaterThanOrderByIdAsc(
            Long parentCommentId, String targetType, Long targetId, Long after, Pageable pageable);

    interface ReplyPreview {
        Long getParentId();
        Long getCommentId();
        int getReplyCount();
    }

    @Query(value = """
            SELECT parent_id AS "parentId", id AS "commentId", reply_count AS "replyCount"
            FROM (
                SELECT reply.parent_comment_id AS parent_id, reply.id,
                       row_number() OVER (PARTITION BY reply.parent_comment_id ORDER BY reply.id DESC) AS position,
                       count(*) FILTER (WHERE reply.deleted_at IS NULL) OVER (PARTITION BY reply.parent_comment_id) AS reply_count
                FROM comments reply JOIN comments parent ON parent.id = reply.parent_comment_id
                WHERE parent.id IN (:parentIds)
                    AND reply.target_type = parent.target_type AND reply.target_id = parent.target_id
            ) ranked
            WHERE position <= :limit AND reply_count > 0
            ORDER BY parent_id, id DESC
            """, nativeQuery = true)
    List<ReplyPreview> findReplyPreviews(@Param("parentIds") List<Long> parentIds, @Param("limit") int limit);

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.id IN :ids")
    List<Comment> findByIdInFetchUser(@Param("ids") List<Long> ids);
}
