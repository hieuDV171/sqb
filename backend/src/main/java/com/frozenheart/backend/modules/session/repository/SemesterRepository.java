package com.frozenheart.backend.modules.session.repository;

import com.frozenheart.backend.core.entity.session.Semester;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Semester s WHERE s.active = true")
    Optional<Semester> findActiveForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Semester s WHERE s.id = :id")
    Optional<Semester> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT id FROM semesters
            WHERE id IN (:ids) AND active = true AND is_finalized = false
            ORDER BY id FOR NO KEY UPDATE
            """, nativeQuery = true)
    List<Long> lockOpenSemesterIds(@Param("ids") Set<Long> ids);

    List<Semester> findAllByOrderByIdDesc();

}
