package com.frozenheart.backend.modules.cosmetic.repository;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticRarity;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import com.frozenheart.backend.core.entity.cosmetic.UserCosmetic;
import com.frozenheart.backend.core.entity.cosmetic.UserCosmeticId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCosmeticRepository extends JpaRepository<UserCosmetic, UserCosmeticId> {

    @Query("""
        SELECT uc
        FROM UserCosmetic uc
        JOIN FETCH uc.cosmetic c
        WHERE uc.id.userId = :userId
          AND (:type IS NULL OR c.type = :type)
          AND (:rarity IS NULL OR c.rarity = :rarity)
          AND (:cursor IS NULL OR c.id < :cursor)
        ORDER BY c.id DESC
        """)
    List<UserCosmetic> findInventoryCursor(
            @Param("userId") Long userId,
            @Param("type") CosmeticType type,
            @Param("rarity") CosmeticRarity rarity,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @Query("""
        SELECT uc
        FROM UserCosmetic uc
        JOIN FETCH uc.cosmetic c
        WHERE uc.id.userId = :userId
          AND uc.equippedAt IS NOT NULL
        """)
    List<UserCosmetic> findEquippedCosmetics(@Param("userId") Long userId);

    @Query("""
        SELECT uc
        FROM UserCosmetic uc
        JOIN FETCH uc.cosmetic c
        WHERE uc.id.userId = :userId
          AND c.type = :type
          AND uc.equippedAt IS NOT NULL
        """)
    Optional<UserCosmetic> findEquippedItemByType(
            @Param("userId") Long userId,
            @Param("type") CosmeticType type);

    @Query("""
        SELECT uc
        FROM UserCosmetic uc
        JOIN FETCH uc.cosmetic c
        WHERE uc.id.userId = :userId
          AND uc.id.cosmeticId = :cosmeticId
        """)
    Optional<UserCosmetic> findByUserIdAndCosmeticId(
            @Param("userId") Long userId,
            @Param("cosmeticId") Long cosmeticId);

    int countByIdUserId(Long userId);

    @Query("""
        SELECT c.rarity, COUNT(uc)
        FROM UserCosmetic uc
        JOIN uc.cosmetic c
        WHERE uc.id.userId = :userId
        GROUP BY c.rarity
        """)
    List<Object[]> countByUserIdGroupByRarity(@Param("userId") Long userId);

    boolean existsByIdUserIdAndIdCosmeticId(Long userId, Long cosmeticId);
}
