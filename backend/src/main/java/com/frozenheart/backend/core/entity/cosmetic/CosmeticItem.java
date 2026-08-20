package com.frozenheart.backend.core.entity.cosmetic;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cosmetic_items")
public class CosmeticItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CosmeticType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CosmeticRarity rarity;

    @Column(length = 500)
    private String assetUrl;

    private double originalPrice;
    private double price;

    private LocalDateTime availableUtil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private CosmeticPrerequisites prerequisites;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // --------------------------

    // --------------------------

    @Singular
    @OneToMany(mappedBy = "cosmetic")
    private Set<UserCosmetic> userCosmetics;



}
