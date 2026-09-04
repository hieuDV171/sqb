package com.frozenheart.backend.modules.exam.service;

import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;

public interface DocumentExportService {

    /**
     * Export all original questions of a subject for students
     */
    ExportResponse exportOriginalQuestions(Long subjectId, String format, Boolean includeAnswer);

    /**
     * Export a generated exam for lecturers
     */
    ExportResponse exportExam(Exam exam, String format, Boolean includeAnswerKey, String paperSize);
}
