package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.user.CourseClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseClassRepository extends JpaRepository<CourseClass, Long> {

    List<CourseClass> findByLecturerId(Long lecturerId);

    List<CourseClass> findBySubjectId(Long subjectId);

}
