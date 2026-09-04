package com.frozenheart.backend.modules.activityfeed.repository;

import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ActivityFeed> findByIdLessThanOrderByIdDesc(Long after, Pageable pageable);

    int countByCreatedAtAfter(LocalDateTime since);

    int countByIdGreaterThan(Long afterId);
}
