package com.frozenheart.backend.modules.device.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.user.UserDevice;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByDeviceId(String deviceId);

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByFid(String fid);

    @Query("SELECT ud FROM UserDevice ud JOIN FETCH ud.user WHERE ud.deviceId = :deviceId AND ud.isActive = true")
    List<UserDevice> findActiveDevicesByDeviceIdWithUser(@Param("deviceId") String deviceId);

    List<UserDevice> findByUserIdOrderByLastActiveAtDesc(Long userId);

    List<UserDevice> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id IN :userIds AND ud.isActive = true")
    List<UserDevice> findActiveDevicesByUserIds(@Param("userIds") Collection<Long> userIds);

}
