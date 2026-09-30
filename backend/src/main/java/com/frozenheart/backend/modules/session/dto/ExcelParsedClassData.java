package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record ExcelParsedClassData(
        String semesterName,
        String department,
        String classCode,
        String classType,
        String subjectCode,
        String subjectName,
        String lecturerName,
        List<ExcelStudentRow> students
) {}
