package com.frozenheart.backend.modules.exam.service;

import com.frozenheart.backend.modules.exam.dto.ExamDetailResponse;
import com.frozenheart.backend.modules.exam.dto.ExamListResponse;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.GenerateExamRequest;

public interface ExamService {

    ExamDetailResponse generateExam(GenerateExamRequest request);

    ExamDetailResponse getExamDetail(Long examId);

    ExamListResponse getMyExams(Long subjectId, Long after, int limit);

    ExportResponse exportExam(Long examId, String format, Boolean includeAnswerKey, String paperSize);
}
