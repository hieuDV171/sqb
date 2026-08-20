package com.frozenheart.backend.modules.device.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.user.UserDevice;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<UserDevice> findByUserIdOrderByLastActiveAtDesc(Long userId);

}
