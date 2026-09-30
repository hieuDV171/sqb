package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    private String targetType;
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ViolationType violationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<MediaItem> evidenceUrls;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReactionType type;

    @Column(length = 500)
    private String description;

    @Column(updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReportStatus status;

    private Instant reviewedAt;

    @Column(length = 500)
    private String resolutionNote;

    // ---------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    // ---------------------

}
