package com.frozenheart.backend.modules.exam.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuestionExportController {

    private final DocumentExportService documentExportService;

    @GetMapping("/questions/export")
    public ResponseEntity<GlobalResponse<ExportResponse>> exportOriginalQuestions(
            @RequestParam(name = "subject_id") Long subjectId,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "true", name = "include_answer") Boolean includeAnswer) {

        ExportResponse response = documentExportService.exportOriginalQuestions(subjectId, format, includeAnswer);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
