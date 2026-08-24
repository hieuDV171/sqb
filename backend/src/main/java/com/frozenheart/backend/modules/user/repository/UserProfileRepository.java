package com.frozenheart.backend.modules.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.user.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user WHERE up.userId = :userId")
    Optional<UserProfile> findByUserIdWithUser(@Param("userId") Long userid);

    @Modifying
    @Query("UPDATE UserProfile up SET up.totalProposedQuestion = up.totalProposedQuestion + :delta WHERE up.userId = :userId")
    int incrementProposedQuestions(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.totalApprovedQuestions = up.totalApprovedQuestions + :approvedDelta WHERE up.userId = :userId")
    int incrementApprovedQuestions(@Param("userId") Long userId, @Param("approvedDelta") int approvedDelta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.badgesCount = up.badgesCount + :delta WHERE up.userId = :userId")
    int incrementBadgesCount(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.friendsCount = up.friendsCount + :delta WHERE up.userId = :userId")
    int incrementFriendsCount(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.followersCount = up.followersCount + :delta WHERE up.userId = :userId")
    int incrementFollowersCount(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.followingCount = up.followingCount + :delta WHERE up.userId = :userId")
    int incrementFollowingCount(@Param("userId") Long userId, @Param("delta") int delta);
}
