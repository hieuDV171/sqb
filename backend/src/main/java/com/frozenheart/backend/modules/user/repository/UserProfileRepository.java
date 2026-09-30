package com.frozenheart.backend.modules.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

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

    List<UserProfile> findAllByStudentLecturerCodeIn(Collection<String> studentCodes);

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user u WHERE LOWER(TRIM(up.fullName)) = LOWER(TRIM(:fullName)) AND u.role = 'LECTURER' AND u.active = true")
    List<UserProfile> findActiveLecturersByFullName(@Param("fullName") String fullName);

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user u WHERE u.verified = true AND u.active = true")
    java.util.List<UserProfile> findAllVerifiedActiveUsersForSearch();

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user u WHERE up.userId IN :ids AND u.verified = true AND u.active = true")
    java.util.List<UserProfile> findByIdInVerifiedActiveUsersForSearch(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE UserProfile up SET up.totalProposedQuestion = up.totalProposedQuestion + :delta WHERE up.userId = :userId")
    void incrementProposedQuestions(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.totalApprovedQuestions = up.totalApprovedQuestions + :approvedDelta WHERE up.userId = :userId")
    int incrementApprovedQuestions(@Param("userId") Long userId, @Param("approvedDelta") int approvedDelta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.badgesCount = up.badgesCount + :delta WHERE up.userId = :userId")
    void incrementBadgesCount(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.friendsCount = up.friendsCount + :delta WHERE up.userId = :userId")
    void incrementFriendsCount(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.followersCount = up.followersCount + :delta WHERE up.userId = :userId")
    void incrementFollowersCount(@Param("userId") Long userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE UserProfile up SET up.followingCount = up.followingCount + :delta WHERE up.userId = :userId")
    void incrementFollowingCount(@Param("userId") Long userId, @Param("delta") int delta);
}
