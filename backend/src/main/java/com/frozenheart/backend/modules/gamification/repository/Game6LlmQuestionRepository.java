package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.Game6LlmQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Game6LlmQuestionRepository extends JpaRepository<Game6LlmQuestion, Long> {

    List<Game6LlmQuestion> findByIdIn(List<Long> ids);

}
