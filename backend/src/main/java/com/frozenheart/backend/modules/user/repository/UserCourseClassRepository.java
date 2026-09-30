package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.user.UserCourseClass;
import com.frozenheart.backend.core.entity.user.UserCourseClassId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface UserCourseClassRepository extends JpaRepository<UserCourseClass, UserCourseClassId> {

    List<UserCourseClass> findByIdUserId(Long userId);

    List<UserCourseClass> findByIdCourseClassId(Long courseClassId);

    boolean existsByIdUserIdAndIdCourseClassId(Long userId, Long courseClassId);

    @Query("SELECT ucc FROM UserCourseClass ucc JOIN FETCH ucc.courseClass cc JOIN FETCH cc.subject sub LEFT JOIN FETCH cc.semester LEFT JOIN FETCH cc.lecturer WHERE ucc.id.userId = :userId")
    List<UserCourseClass> findByUserIdFetchCourseClassAndSubject(@Param("userId") Long userId);

    @Query("SELECT ucc FROM UserCourseClass ucc WHERE ucc.id.courseClassId = :courseClassId AND ucc.id.userId IN :userIds")
    List<UserCourseClass> findByIdCourseClassIdAndIdUserIdIn(@Param("courseClassId") Long courseClassId, @Param("userIds") Collection<Long> userIds);

    @Query("SELECT ucc FROM UserCourseClass ucc JOIN FETCH ucc.user u WHERE ucc.id.courseClassId = :courseClassId")
    List<UserCourseClass> findByCourseClassIdFetchUser(@Param("courseClassId") Long courseClassId);

}
