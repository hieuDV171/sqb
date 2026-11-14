package com.frozenheart.backend.modules.report.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.socialinteraction.Report;
import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.dto.ReportResponseDto;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import com.frozenheart.backend.modules.report.service.ReportEvidenceStorage;
import com.frozenheart.backend.modules.report.service.ReportService;
import com.frozenheart.backend.modules.report.service.ReportTargets;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reports;
    private final UserRepository users;
    private final ReportTargets targets;
    private final ReportEvidenceStorage evidence;

    @Override
    @Transactional
    public Submission createReport(CreateReportRequestDto request) {
        JwtPayload viewer = JwtPayload.getCurrentUserPayload();
        // Serialize submissions by reporter, including concurrent retries and the rate-limit check.
        var reporter = users.findByIdForUpdate(viewer.getUserId())
                .filter(user -> user.isActive() && !user.isSuspendedAt(Instant.now()))
                .orElseThrow(() -> new AppException(ResponseCode.ACCESS_DENIED));
        var existing = reports.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatusOrderByIdDesc(
                reporter.getId(), request.getTargetType().name(), request.getTargetId(), ReportStatus.PENDING);
        if (existing.isPresent()) return response(existing.get(), request, false);

        Instant now = Instant.now();
        if (reports.countByReporterIdAndCreatedAtAfter(reporter.getId(), now.minus(1, ChronoUnit.HOURS)) >= 10) {
            throw new AppException(ResponseCode.REPORT_RATE_LIMITED, "Bạn đã gửi nhiều báo cáo. Hãy thử lại sau.");
        }

        var snapshot = targets.snapshotForSubmission(request.getTargetType(), request.getTargetId(), viewer);
        String key = evidence.attach(reporter.getId(), request.getEvidenceKeys(), request.getEvidenceUrls());
        Report saved = reports.save(Report.builder().reporter(reporter)
                .targetType(request.getTargetType().name()).targetId(request.getTargetId())
                .violationType(request.getViolationType()).description(request.getDescription())
                .snapshot(snapshot).privateEvidenceKey(key).status(ReportStatus.PENDING).createdAt(now).build());
        return response(saved, request, true);
    }

    private static Submission response(Report report, CreateReportRequestDto request, boolean created) {
        return new Submission(ReportResponseDto.builder().reportId(report.getId()).targetType(request.getTargetType())
                .targetId(report.getTargetId()).status(report.getStatus()).createdAt(report.getCreatedAt()).build(), created);
    }
}
