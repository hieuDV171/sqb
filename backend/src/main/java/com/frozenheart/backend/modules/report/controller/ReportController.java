package com.frozenheart.backend.modules.report.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.dto.ReportResponseDto;
import com.frozenheart.backend.modules.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/report")
    public ResponseEntity<GlobalResponse<ReportResponseDto>> createReport(@Valid @RequestBody CreateReportRequestDto request) {
        ReportResponseDto response = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
    }
}
