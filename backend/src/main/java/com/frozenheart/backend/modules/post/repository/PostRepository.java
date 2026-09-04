package com.frozenheart.backend.modules.post.repository;

import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying
    @Query("UPDATE Post p SET p.reactCount = p.reactCount + :delta WHERE p.id = :postId")
    int incrementReactCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :postId")
    int incrementCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    @EntityGraph(attributePaths = {"poster", "subject", "notedLecturer", "session"})
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT p FROM Post p JOIN FETCH p.poster u WHERE p.deletedAt IS NULL AND p.visibility != com.frozenheart.backend.core.entity.post.PostVisibility.ONLY_ME")
    List<Post> findAllPublicPostsForSearch();

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
}
