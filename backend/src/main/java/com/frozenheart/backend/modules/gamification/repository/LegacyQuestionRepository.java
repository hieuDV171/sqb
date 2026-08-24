package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.session.LegacyQuestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegacyQuestionRepository extends JpaRepository<LegacyQuestion, Long> {

    List<LegacyQuestion> findByUsedFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE LegacyQuestion l SET l.used = false")
    void resetAllIsUsedToFalse();

}
