package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserFollow;
import com.frozenheart.backend.core.entity.socialinteraction.UserFollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    Optional<UserFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    Boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
}
