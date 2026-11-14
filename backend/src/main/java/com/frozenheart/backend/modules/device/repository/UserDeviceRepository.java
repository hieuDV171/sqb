package com.frozenheart.backend.modules.device.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.user.UserDevice;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByDeviceId(String deviceId);

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(concat('sqb:fid:', :fid), 0))",
            nativeQuery = true)
    int lockFidForTransaction(@Param("fid") String fid);

    @Query("""
            SELECT COUNT(d) > 0 FROM UserDevice d
            WHERE d.fid = :fid AND (d.user.id IS DISTINCT FROM :userId OR d.deviceId IS DISTINCT FROM :deviceId)
            """)
    boolean isFidBoundToAnotherDevice(@Param("fid") String fid, @Param("userId") Long userId,
                                      @Param("deviceId") String deviceId);

    @Modifying
    @Query("""
            UPDATE UserDevice d SET d.fid = null
            WHERE d.user.id = :userId AND d.deviceId = :deviceId AND d.fid = :fid
            """)
    int clearMatchingFid(@Param("userId") Long userId, @Param("deviceId") String deviceId,
                         @Param("fid") String fid);

    @Modifying
    @Query("""
            UPDATE UserDevice d SET d.fid = :fid, d.lastActiveAt = :now
            WHERE d.user.id = :userId AND d.deviceId = :deviceId AND d.isActive = true
            """)
    int syncActiveDeviceFid(@Param("userId") Long userId, @Param("deviceId") String deviceId,
                           @Param("fid") String fid, @Param("now") Instant now);

    List<UserDevice> findByUserIdOrderByLastActiveAtDesc(Long userId);

    @Modifying
    @Query("UPDATE UserDevice d SET d.isActive = false, d.fid = null WHERE d.user.id IN :userIds")
    void deactivateForAccounts(@Param("userIds") Collection<Long> userIds);

    List<UserDevice> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id IN :userIds AND ud.isActive = true")
    List<UserDevice> findActiveDevicesByUserIds(@Param("userIds") Collection<Long> userIds);

}
