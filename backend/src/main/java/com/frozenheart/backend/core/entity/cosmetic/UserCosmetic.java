package com.frozenheart.backend.core.entity.cosmetic;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_cosmetics")
public class UserCosmetic {

    @EmbeddedId
    private UserCosmeticId id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CosmeticAcquireMethod acquireMethod;

    private Instant acquireAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private AcquireMetadata metadata;

    private Instant equippedAt;

    // ----------------------------
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("cosmeticId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cosmetic_id")
    private CosmeticItem cosmetic;

    // -----------------------------

}
