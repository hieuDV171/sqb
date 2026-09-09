package com.frozenheart.backend.modules.search.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Search;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<Search, Long> {

    @Query("SELECT s FROM Search s WHERE s.user.id = :userId ORDER BY s.lastSearchedAt DESC")
    List<Search> findRecentSearchesByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Search s SET s.lastSearchedAt = :now WHERE s.user.id = :userId AND s.queryText = :queryText")
    int updateLastSearchedAt(@Param("userId") Long userId, @Param("queryText") String queryText,
            @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM Search s WHERE s.id = :id AND s.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM searches WHERE user_id = :userId AND id NOT IN (SELECT id FROM searches WHERE user_id = :userId ORDER BY last_searched_at DESC LIMIT :keepLimit)", nativeQuery = true)
    void pruneOldSearches(@Param("userId") Long userId, @Param("keepLimit") int keepLimit);
}
