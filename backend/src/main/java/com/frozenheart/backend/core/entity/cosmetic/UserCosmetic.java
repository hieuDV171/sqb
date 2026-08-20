package com.frozenheart.backend.core.entity.cosmetic;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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


    private String acquireMethod; // Chưa được định nghĩa

    private LocalDateTime acquireAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private AcquireMetadata metadata;

    private LocalDateTime equippedAt;

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
