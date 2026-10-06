package com.frozenheart.backend.modules.exam.repository;

import com.frozenheart.backend.core.entity.user.CourseClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseClassRepository extends JpaRepository<CourseClass, Long> {

    @Query(
            """
        SELECT cc
        FROM CourseClass cc
        WHERE cc.id IN :ids
"""
    )
    List<CourseClass> findAllByIds(@Param("ids") List<Long> ids);

    Optional<CourseClass> findByClassCodeAndSemesterId(String classCode, Long semesterId);

    boolean existsByClassCodeAndSemesterId(String classCode, Long semesterId);

    boolean existsBySemesterId(Long semesterId);

    @Query("SELECT cc FROM CourseClass cc JOIN FETCH cc.subject sub JOIN FETCH cc.semester sem LEFT JOIN FETCH cc.lecturer l WHERE cc.id = :id")
    Optional<CourseClass> findByIdFetchAll(@Param("id") Long id);

    @Query("SELECT cc FROM CourseClass cc JOIN FETCH cc.subject sub JOIN FETCH cc.semester sem LEFT JOIN FETCH cc.lecturer l WHERE cc.semester.id = :semesterId ORDER BY cc.classCode ASC")
    List<CourseClass> findBySemesterIdFetchAll(@Param("semesterId") Long semesterId);

    @Query("SELECT cc FROM CourseClass cc JOIN FETCH cc.subject sub JOIN FETCH cc.semester sem LEFT JOIN FETCH cc.lecturer l WHERE cc.lecturer.id = :lecturerId AND cc.semester.id = :semesterId ORDER BY cc.classCode ASC")
    List<CourseClass> findByLecturerIdAndSemesterIdFetchAll(@Param("lecturerId") Long lecturerId, @Param("semesterId") Long semesterId);

}
