package com.frozenheart.backend.modules.exam.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.exam.dto.ExamDetailResponse;
import com.frozenheart.backend.modules.exam.dto.ExamListResponse;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.GenerateExamRequest;
import com.frozenheart.backend.modules.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exams")
public class ExamController {

    private final ExamService examService;

    @PostMapping("/generate")
    public ResponseEntity<GlobalResponse<ExamDetailResponse>> generateExam(
            @Valid @RequestBody GenerateExamRequest request
        ) {

        ExamDetailResponse response = examService.generateExam(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<GlobalResponse<ExamDetailResponse>> getExamDetail(
            @PathVariable Long examId) {

        ExamDetailResponse response = examService.getExamDetail(examId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/my-exams")
    public ResponseEntity<GlobalResponse<ExamListResponse>> getMyExams(
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false, name = "subject_id") Long subjectId
            ) {

        ExamListResponse response = examService.getMyExams(subjectId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/{examId}/export")
    public ResponseEntity<GlobalResponse<ExportResponse>> exportExam(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false", name = "include_answer_key") Boolean includeAnswerKey,
            @RequestParam(defaultValue = "A4", name = "paper_size") String paperSize) {

        ExportResponse response = examService.exportExam(examId, format, includeAnswerKey, paperSize);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
