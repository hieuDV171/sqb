package com.frozenheart.backend.modules.session.repository;

import com.frozenheart.backend.core.entity.session.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByActiveTrue();

    List<Semester> findAllByOrderByIdDesc();

    @Modifying
    @Query("UPDATE Semester s SET s.active = false WHERE s.active = true")
    void deactivateAllSemesters();

}
