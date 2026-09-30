package com.frozenheart.backend.core.entity.user;

import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_classes")
public class CourseClass {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, length = 50)
    private String classCode;


    @Column(updatable = false)
    private Instant createdAt;

    // ----------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    // ----------------------

    @Singular
    @OneToMany(mappedBy = "courseClass", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserCourseClass> studentEnrollments;

    @Singular
    @OneToMany(mappedBy = "courseClass")
    private Set<ExamCourseClass> examCourseClasses;
}
