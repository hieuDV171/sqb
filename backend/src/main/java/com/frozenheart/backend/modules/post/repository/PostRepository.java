package com.frozenheart.backend.modules.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.frozenheart.backend.core.entity.post.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying
    @Query("UPDATE Post p SET p.reactCount = p.reactCount + :delta WHERE p.id = :postId")
    int incrementReactCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :postId")
    int incrementCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

}
