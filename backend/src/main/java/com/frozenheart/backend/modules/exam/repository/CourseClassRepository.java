package com.frozenheart.backend.modules.exam.repository;

import com.frozenheart.backend.core.entity.user.CourseClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseClassRepository extends JpaRepository<CourseClass, Long> {

    @Query(
            """
        SELECT cc
        FROM CourseClass cc
        WHERE cc.id IN :ids
"""
    )
    List<CourseClass> findAllByIds(@Param("ids") List<Long> ids);

}
