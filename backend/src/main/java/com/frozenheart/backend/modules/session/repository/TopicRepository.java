package com.frozenheart.backend.modules.session.repository;

import com.frozenheart.backend.core.entity.session.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectIdOrderByNameAsc(Long subjectId);

    Optional<Topic> findByIdAndSubjectId(Long id, Long subjectId);

    List<Topic> findBySubjectIdAndIdIn(Long subjectId, Collection<Long> ids);

    @Query("SELECT COUNT(c) FROM CourseClass c WHERE c.subject.id = :subjectId AND c.lecturer.id = :lecturerId")
    long countTeachingAssignments(Long subjectId, Long lecturerId);
}
