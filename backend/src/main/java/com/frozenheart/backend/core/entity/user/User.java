package com.frozenheart.backend.core.entity.user;

import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import com.frozenheart.backend.core.entity.ai.AiChatSession;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.conversation.UserDeletedMessageOnlyMe;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.badge.UserBadge;
import com.frozenheart.backend.core.entity.cosmetic.UserCosmetic;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.prediction.PointHistory;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLog;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.socialinteraction.*;
import com.frozenheart.backend.core.entity.session.Session;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(length = 10)
    private String gender;

    private LocalDate dateOfBirth;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ----------------------

    // ----------------------

    @Singular
    @OneToMany(mappedBy = "lecturer")
    private Set<CourseClass> taughtClasses;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<UserCourseClass> classEnrollments;

    @Singular
    @OneToMany(mappedBy = "sender")
    private Set<Friendship> invites;

    @Singular
    @OneToMany(mappedBy = "receiver")
    private Set<Friendship> receivedInvites;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<UserRating> ratedQuestions;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<UserAnswer> answeredQuestions;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<UserDeletedMessageOnlyMe> deletedMessageOnlyMes;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<UserParticipant> conversations;

    @Singular
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserCosmetic> userCosmetics;

    @Singular
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserBadge> userBadges;

    @Singular("aFollowing")
    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserFollow> following;

    @Singular
    @OneToMany(mappedBy = "followedUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserFollow> followers;

    @Singular("aBlocking")
    @OneToMany(mappedBy = "blocker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserBlock> blocking;

    @Singular
    @OneToMany(mappedBy = "blocked", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserBlock> blockers;

    @Singular
    @OneToMany(mappedBy = "actor")
    private Set<Notification> sentNotifications;

    @Singular
    @OneToMany(mappedBy = "receiver")
    private Set<Notification> receivedNotifications;

    @Singular
    @OneToMany(mappedBy = "proposer")
    private Set<Session> sessions;

    @Singular
    @OneToMany(mappedBy = "poster")
    private Set<Post> posts;

    @Singular
    @OneToMany(mappedBy = "actor")
    private Set<QuestionEditLog> questionEditLogs;

    @Singular
    @OneToMany(mappedBy = "lecturer")
    private Set<Exam> exams;

    @Singular
    @OneToMany(mappedBy = "notedLecturer")
    private Set<Post> notes;

    @Singular
    @OneToMany(mappedBy = "owner")
    private Set<QuestionMedia> medias;

    @Singular
    @OneToMany(mappedBy = "gambler")
    private Set<Prediction> predictions;

    @Singular
    @OneToMany(mappedBy = "reviewer")
    private Set<Session> reviewedSessions;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<Comment> comments;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<React> reacts;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<ActivityFeed> activityFeeds;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<PointHistory> pointHistories;

    @Singular
    @OneToMany(mappedBy = "reporter")
    private Set<Report> reports;

    @Singular
    @OneToMany(mappedBy = "reviewer")
    private Set<Report> adminReports;

    @Singular
    @OneToMany(mappedBy = "creater")
    private Set<Conversation> createdConversations;

    @Singular
    @OneToMany(mappedBy = "sender")
    private Set<Message> sentMessages;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<Search> searches;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<UserDevice> devices;

    @Singular
    @OneToMany(mappedBy = "user")
    private Set<AiChatSession> aiChatSessions;

}
