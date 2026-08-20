package com.frozenheart.backend.core.entity.session;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // "Mã môn"_"Mã lớp"_"Tên kỳ thi"_"examId"
    @Column(comment = "subjectId_classId_examName_examId")
    private String examCode;

    private String title;
    private String description;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ---------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private User lecturer;
    // ---------------------------

}
