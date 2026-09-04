package com.frozenheart.backend.core.entity.session;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ExamQuestionId implements Serializable {

    private Long examId;

    private Long questionId;
}
