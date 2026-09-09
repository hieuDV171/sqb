package com.frozenheart.backend.core.entity.badge;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)

    @Column(length = 20)
    private BadgeTier badgeTier;

    // Sự kiện kích hoạt huy hiệu
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BadgeTriggerEvent badgeTriggerEvent;

    private String iconUrl;

    // Loại thuật toán kích hoạt huy hiệu
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private BadgeCriteria criteria;

    @Column(updatable = false)
    private Instant createdAt;

    @Builder.Default
    private boolean active = true;

    // ---------------------------

    // ---------------------------

    @Singular
    @OneToMany(mappedBy = "badge")
    private Set<UserBadge> userBadges;

}
