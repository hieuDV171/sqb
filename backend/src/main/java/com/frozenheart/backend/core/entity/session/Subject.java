package com.frozenheart.backend.core.entity.session;

import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.prediction.PointHistory;
import com.frozenheart.backend.core.entity.user.CourseClass;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 10, nullable = false, unique = true)
    private String code;

    

    // --------------------

    // --------------------
    @Singular
    @OneToMany(mappedBy = "subject")
    private Set<Topic> topics;

    @Singular
    @OneToMany(mappedBy = "subject")
    private Set<Session> sessions;

    @Singular
    @OneToMany(mappedBy = "subject")
    private Set<Post> posts;

    @Singular
    @OneToMany(mappedBy = "subject")
    private Set<CourseClass> courseClasses;

    @Singular
    @OneToMany(mappedBy = "subject")
    private Set<PointHistory> pointHistories;

    @Singular
    @OneToMany(mappedBy = "subject")
    private Set<Exam> exams;
}
