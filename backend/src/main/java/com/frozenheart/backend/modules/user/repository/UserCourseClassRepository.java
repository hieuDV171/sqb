package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.user.UserCourseClass;
import com.frozenheart.backend.core.entity.user.UserCourseClassId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCourseClassRepository extends JpaRepository<UserCourseClass, UserCourseClassId> {

    List<UserCourseClass> findByIdUserId(Long userId);

    List<UserCourseClass> findByIdCourseClassId(Long courseClassId);

}
