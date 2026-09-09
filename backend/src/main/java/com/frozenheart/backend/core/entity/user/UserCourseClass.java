package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_course_classes")
public class UserCourseClass {

    @EmbeddedId
    private UserCourseClassId id;

    @Column(updatable = false)
    private Instant enrolledAt;

    // ----------------------
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("courseClassId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_class_id")
    private CourseClass courseClass;
    // ----------------------
}
