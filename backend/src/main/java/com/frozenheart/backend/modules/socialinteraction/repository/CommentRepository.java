package com.frozenheart.backend.modules.socialinteraction.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<Comment> findByParentCommentIdAndIdLessThanOrderByIdDesc(
            Long parentCommentId, Long after, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByParentCommentIdAndIdGreaterThanOrderByIdAsc(
            Long parentCommentId, Long after, Pageable pageable);

    int countByParentCommentIdAndDeletedAtIsNull(Long parentCommentId);
}
