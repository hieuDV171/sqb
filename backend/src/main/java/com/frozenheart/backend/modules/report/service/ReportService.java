package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.dto.ReportResponseDto;

public interface ReportService {
    ReportResponseDto createReport(CreateReportRequestDto request);
}
