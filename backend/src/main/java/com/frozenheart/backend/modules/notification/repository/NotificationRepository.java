package com.frozenheart.backend.modules.notification.repository;

import com.frozenheart.backend.core.entity.notification.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n
        FROM Notification n
        LEFT JOIN FETCH n.actor a
        WHERE n.receiver.id = :receiverId
          AND (:cursor IS NULL OR n.id < :cursor)
        ORDER BY n.id DESC
        """)
    List<Notification> findNotificationsCursor(
            @Param("receiverId") Long receiverId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @Query("""
        SELECT COUNT(n)
        FROM Notification n
        WHERE n.receiver.id = :receiverId
          AND n.readAt IS NULL
        """)
    int countUnreadNotifications(@Param("receiverId") Long receiverId);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.readAt = :now
        WHERE n.receiver.id = :receiverId
          AND n.readAt IS NULL
        """)
    int markAllAsRead(
            @Param("receiverId") Long receiverId,
            @Param("now") LocalDateTime now);
}
