package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record ExcelImportClassResult(
        Long courseClassId,
        String classCode,

        String subjectCode,
        String subjectName,

        String semesterName,

        int totalRowsInFile,

        int newUsersCreated,
        int existingUsersFound,
        
        int newEnrollments,
        int alreadyEnrolledCount
    ) {
}
