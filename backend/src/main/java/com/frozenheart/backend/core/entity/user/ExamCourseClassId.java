package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class ExamCourseClassId implements Serializable {

    private Long examId;
    private Long courseClassId;

}
