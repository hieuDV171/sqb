package com.frozenheart.backend.modules.exam.repository;

import com.frozenheart.backend.core.entity.user.ExamCourseClass;
import com.frozenheart.backend.core.entity.user.ExamCourseClassId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamCourseClassRepository extends JpaRepository<ExamCourseClass, ExamCourseClassId> {

}
