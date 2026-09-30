package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.time.Instant;

@Builder
public record CourseClassResponse(
        Long courseClassId,
        String classCode,

        Long subjectId,
        String subjectCode,
        String subjectName,

        Long semesterId,
        String semesterName,

        Long lecturerId,
        String lecturerName,

        int totalStudents,
        Instant createdAt
    ) {
}
