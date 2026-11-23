package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.session.Subject;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Subject s WHERE s.id = :id")
    java.util.Optional<Subject> lockTopicCatalog(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Subject s WHERE s.id IN :ids ORDER BY s.id")
    List<Subject> lockForAutoReview(java.util.Collection<Long> ids);

    Optional<Subject> findByCode(String code);

    List<Subject> findAllByOrderByNameAsc();

}

