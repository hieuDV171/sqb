package com.frozenheart.backend.core.entity.post;

import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(value = EnumType.STRING)
    private PostType postType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<MediaItem> mediaUrls;

    @Enumerated(value = EnumType.STRING)
    private PostVisibility visibility;

    @Column(updatable = false)
    private LocalDateTime creadtedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private int reactCount;
    private int commentCount;

    private String lecturerNote;
    private LocalDateTime lecturerNoteAddedAt;

    // ---------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id")
    private User poster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noted_lecturer_id")
    private User notedLecturer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;
    // ---------------------------

}
