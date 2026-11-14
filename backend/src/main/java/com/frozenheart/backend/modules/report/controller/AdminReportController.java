package com.frozenheart.backend.modules.report.controller;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.core.entity.socialinteraction.ViolationType;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import com.frozenheart.backend.modules.report.dto.AdminReportDto.*;
import com.frozenheart.backend.modules.report.service.AdminReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Báo cáo vi phạm · Quản trị")
public class AdminReportController {
    private final AdminReportService reports;

    @GetMapping("/reports")
    public GlobalResponse<CursorResponse<Summary>> queue(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(name = "target_type", required = false) ReportTargetType targetType,
            @RequestParam(name = "violation_type", required = false) ViolationType violationType,
            @RequestParam(required = false) Long after, @RequestParam(required = false) Integer limit) {
        ReportStatus filter = switch (status) {
            case "ALL" -> null;
            case "PENDING" -> ReportStatus.PENDING;
            case "RESOLVED" -> ReportStatus.RESOLVED;
            default -> throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
        };
        return GlobalResponse.success(reports.queue(filter, targetType, violationType, after, limit));
    }

    @GetMapping("/reports/{id}")
    public GlobalResponse<Detail> detail(@PathVariable Long id) {
        return GlobalResponse.success(reports.detail(id));
    }

    @GetMapping("/reports/{id}/related")
    public GlobalResponse<CursorResponse<Summary>> related(@PathVariable Long id,
            @RequestParam(required = false) Long after, @RequestParam(required = false) Integer limit) {
        return GlobalResponse.success(reports.related(id, after, limit));
    }

    @GetMapping("/reports/{id}/evidence")
    public ResponseEntity<GlobalResponse<Evidence>> evidence(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(GlobalResponse.success(reports.evidence(id)));
    }

    @PostMapping("/reports/{id}/evidence/import")
    public ResponseEntity<GlobalResponse<Evidence>> importEvidence(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(GlobalResponse.success(reports.importEvidence(id)));
    }

    @PostMapping("/reports/{id}/resolve")
    public GlobalResponse<Detail> resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return GlobalResponse.success(reports.resolve(id, request));
    }

    @PostMapping("/moderation-actions/{id}/reverse")
    public GlobalResponse<Detail> reverse(@PathVariable Long id, @Valid @RequestBody ReverseRequest request) {
        return GlobalResponse.success(reports.reverse(id, request));
    }
}
