package com.frozenheart.backend.modules.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.user.UserPushSetting;

@Repository
public interface UserPushSettingRepository extends JpaRepository<UserPushSetting, Long> {

}
