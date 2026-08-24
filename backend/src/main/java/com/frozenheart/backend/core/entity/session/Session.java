package com.frozenheart.backend.core.entity.session;

import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLog;
import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // "Mã môn"_"Mã tác giả"_"Timestamp"_"sessionId"
    @Column(comment = "subjectCode_authorCode_timestampId_sessionId")
    private String sessionCode;

    private String sourceUrl;
    private String title;
    private String content;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 20)
    private SessionStatus status;

    private int reactCount;
    private int commentCount;

    @Builder.Default
    private boolean anonymous = false;

    private LocalDateTime reviewedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ---------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id")
    private User proposer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
    // ---------------------------

    @Singular
    @OneToMany(mappedBy = "session")
    private Set<QuestionEditLog> questionEditLogs;

    @Singular
    @OneToMany(mappedBy = "session")
    private Set<Question> questions;

    @OneToOne(mappedBy = "session")
    private Post post;
}
