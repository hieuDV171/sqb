package com.frozenheart.backend.modules.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.user.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user WHERE up.userId = :userId")
    Optional<UserProfile> findByUserIdWithUser(@Param("userId") Long userid);

}
