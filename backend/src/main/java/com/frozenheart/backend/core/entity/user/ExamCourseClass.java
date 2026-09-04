package com.frozenheart.backend.core.entity.user;

import com.frozenheart.backend.core.entity.session.Exam;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exam_course_classes")
public class ExamCourseClass {

    @EmbeddedId
    private ExamCourseClassId id;

    // "Mã môn"_"Mã lớp"_"Tên kỳ thi"_"examId"
    @Column(comment = "subjectId_classId_examName_examId")
    private String examCode;


    // ----------------------------
    @MapsId("examId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @MapsId("courseClassId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_class_id")
    private CourseClass courseClass;
    // -----------------------------

}
