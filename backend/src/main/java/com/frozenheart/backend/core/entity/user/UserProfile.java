package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String fullName;

    private String avatarUrl;
    private String coverUrl;
    
    private String avatarFrameUrl;
    private String chatBubbleUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String schoolFaculty;

    @Column(length = 100)
    private String major;

    @Column(length = 100)
    private String className;

    @Column(length = 20)
    private String studentLecturerCode;

    @Builder.Default
    private int totalProposedQuestion = 0;

    @Builder.Default
    private int totalApprovedQuestions = 0;

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

    private String hiddenChatPin;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    // ---------------
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    // ---------------
}
