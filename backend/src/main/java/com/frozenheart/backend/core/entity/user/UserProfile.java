package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String fullName;

    private String avatarUrl;
    private String coverUrl;
    
    private String avatarFrameUrl;
    private String chatBubbleUrl;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String faculty;

    @Column(length = 100)
    private String major;

    @Column(length = 20)
    private String studentLecturerCode;

    @Builder.Default
    private int totalProposedQuestion = 0;

    @Builder.Default
    private int totalApprovedQuestions = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    @Builder.Default
    private GamificationPointsJson gamificationPoints = new GamificationPointsJson(0.0, 0.0);

    @Builder.Default
    private int badgesCount = 0;

    @Builder.Default
    private int friendsCount = 0;

    @Builder.Default
    private int followersCount = 0;

    @Builder.Default
    private int followingCount = 0;

    @Column(nullable = false)
    private boolean profileCompleted;

    @Column(length = 255)
    private String hiddenChatPin;

}
