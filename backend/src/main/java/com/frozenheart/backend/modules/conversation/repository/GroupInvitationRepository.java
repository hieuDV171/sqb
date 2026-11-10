package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.GroupInvitation;
import com.frozenheart.backend.core.entity.user.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    @Query("""
            SELECT p FROM UserProfile p JOIN FETCH p.user u
            WHERE p.userId > :after AND p.userId <> :actor AND u.active = true
              AND LOCATE(LOWER(:name), LOWER(p.fullName)) > 0
              AND (
                EXISTS (SELECT 1 FROM Friendship f WHERE f.status = 'ACCEPTED'
                  AND ((f.sender.id = :actor AND f.receiver.id = p.userId)
                    OR (f.receiver.id = :actor AND f.sender.id = p.userId)))
                OR EXISTS (SELECT 1 FROM UserFollow f
                  WHERE (f.follower.id = :actor AND f.followedUser.id = p.userId)
                     OR (f.followedUser.id = :actor AND f.follower.id = p.userId))
              )
              AND NOT EXISTS (SELECT 1 FROM UserBlock b
                  WHERE (b.blocker.id = :actor AND b.blocked.id = p.userId)
                     OR (b.blocked.id = :actor AND b.blocker.id = p.userId))
              AND NOT EXISTS (SELECT 1 FROM UserParticipant m
                  WHERE m.conversation.id = :groupId AND m.user.id = p.userId AND m.leftAt IS NULL)
              AND NOT EXISTS (SELECT 1 FROM GroupInvitation i
                  WHERE i.conversation.id = :groupId AND i.invitee.id = p.userId AND i.expiresAt > :now)
            ORDER BY p.userId
            """)
    List<UserProfile> findCandidates(@Param("actor") Long actor, @Param("groupId") Long groupId,
            @Param("after") long after, @Param("name") String name, @Param("now") Instant now, Pageable pageable);

    @Query("""
            SELECT i FROM GroupInvitation i JOIN FETCH i.conversation JOIN FETCH i.inviter u
            WHERE i.invitee.id = :invitee AND i.id > :after AND i.expiresAt > :now AND u.active = true
              AND EXISTS (SELECT 1 FROM UserParticipant p
                  WHERE p.conversation.id = i.conversation.id AND p.user.id = u.id AND p.leftAt IS NULL)
              AND NOT EXISTS (SELECT 1 FROM UserBlock b
                  WHERE (b.blocker.id = :invitee AND b.blocked.id = u.id)
                     OR (b.blocked.id = :invitee AND b.blocker.id = u.id))
            ORDER BY i.id
            """)
    List<GroupInvitation> findIncoming(@Param("invitee") Long invitee, @Param("after") long after,
            @Param("now") Instant now, Pageable pageable);

    @Query("SELECT i.conversation.id FROM GroupInvitation i WHERE i.id = :id AND i.invitee.id = :invitee")
    Optional<Long> findConversationId(@Param("id") Long id, @Param("invitee") Long invitee);

    @Query("""
            SELECT i FROM GroupInvitation i JOIN FETCH i.inviter JOIN FETCH i.invitee
            WHERE i.id = :id AND i.invitee.id = :invitee
            """)
    Optional<GroupInvitation> findForInvitee(@Param("id") Long id, @Param("invitee") Long invitee);

    @Query("SELECT i.invitee.id FROM GroupInvitation i WHERE i.conversation.id = :groupId AND i.expiresAt > :now")
    Set<Long> findPendingInviteeIds(@Param("groupId") Long groupId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM GroupInvitation i WHERE i.conversation.id = :groupId AND i.expiresAt <= :now")
    void deleteExpiredForGroup(@Param("groupId") Long groupId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM GroupInvitation i WHERE i.id = :id AND i.invitee.id = :invitee")
    int deleteForInvitee(@Param("id") Long id, @Param("invitee") Long invitee);

    @Modifying
    @Query("DELETE FROM GroupInvitation i WHERE i.conversation.id = :groupId AND i.inviter.id = :inviter")
    void deleteForInviter(@Param("groupId") Long groupId, @Param("inviter") Long inviter);
}
