package com.frozenheart.backend.modules.cosmetic.repository;

import com.frozenheart.backend.core.entity.cosmetic.CosmeticItem;
import com.frozenheart.backend.core.entity.cosmetic.CosmeticType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CosmeticItemRepository extends JpaRepository<CosmeticItem, Long> {

  @Query("""
      SELECT COUNT(c)
      FROM CosmeticItem c
      WHERE c.availableUtil IS NULL OR c.availableUtil > :now
      """)
  int countActiveItems(@Param("now") Instant now);

  @Query("""
      SELECT c
      FROM CosmeticItem c
      WHERE (:type IS NULL OR c.type = :type)
        AND (c.availableUtil IS NULL OR c.availableUtil > :now)
        AND (:cursor IS NULL OR c.id < :cursor)
      ORDER BY c.id DESC
      """)
  List<CosmeticItem> findShopItemsNewest(
      @Param("type") CosmeticType type,
      @Param("now") Instant now,
      @Param("cursor") Long cursor,
      Pageable pageable);

  @Query("""
      SELECT c
      FROM CosmeticItem c
      WHERE (:type IS NULL OR c.type = :type)
        AND (c.availableUtil IS NULL OR c.availableUtil > :now)
      ORDER BY c.price ASC, c.id ASC
      """)
  List<CosmeticItem> findShopItemsPriceAsc(
      @Param("type") CosmeticType type,
      @Param("now") Instant now,
      Pageable pageable);

  @Query("""
      SELECT c
      FROM CosmeticItem c
      WHERE (:type IS NULL OR c.type = :type)
        AND (c.availableUtil IS NULL OR c.availableUtil > :now)
      ORDER BY c.price DESC, c.id DESC
      """)
  List<CosmeticItem> findShopItemsPriceDesc(
      @Param("type") CosmeticType type,
      @Param("now") Instant now,
      Pageable pageable);

  @Query("""
      SELECT c
      FROM CosmeticItem c
      WHERE (:type IS NULL OR c.type = :type)
        AND (c.availableUtil IS NULL OR c.availableUtil > :now)
      ORDER BY c.rarity DESC, c.id DESC
      """)
  List<CosmeticItem> findShopItemsRarity(
      @Param("type") CosmeticType type,
      @Param("now") Instant now,
      Pageable pageable);
}
