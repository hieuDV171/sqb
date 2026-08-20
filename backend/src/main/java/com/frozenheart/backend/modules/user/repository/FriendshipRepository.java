package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

        @Query("""
            SELECT f
            FROM Friendship f
            WHERE (f.sender.id = :userId1 AND f.receiver.id = :userId2)
               OR (f.sender.id = :userId2 AND f.receiver.id = :userId1)
            """)
    Optional<Friendship> findFriendshipsBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

}
