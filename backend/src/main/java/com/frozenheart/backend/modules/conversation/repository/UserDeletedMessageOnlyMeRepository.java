package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.UserDeletedMessageOnlyMe;
import com.frozenheart.backend.core.entity.conversation.UserDeletedMessageOnlyMeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface UserDeletedMessageOnlyMeRepository extends JpaRepository<UserDeletedMessageOnlyMe, UserDeletedMessageOnlyMeId> {

    @Query("""
        SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END
        FROM UserDeletedMessageOnlyMe d
        WHERE d.id.userId = :userId AND d.id.messageId = :messageId
        """)
    boolean existsByUserIdAndMessageId(@Param("userId") Long userId, @Param("messageId") Long messageId);
}
