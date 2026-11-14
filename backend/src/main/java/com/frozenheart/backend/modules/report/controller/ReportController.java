package com.frozenheart.backend.modules.report.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.dto.ReportResponseDto;
import com.frozenheart.backend.modules.report.service.ReportService;
import com.frozenheart.backend.modules.report.service.ReportEvidenceStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "26. Báo cáo & Khiếu nại (Reports)", description = "APIs gửi đơn báo cáo vi phạm nội dung hoặc hành vi người dùng")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportEvidenceStorage evidence;

    @PostMapping(value = "/report/evidence", consumes = "multipart/form-data")
    public ResponseEntity<GlobalResponse<Map<String, String>>> uploadEvidence(@RequestPart("file") MultipartFile file) {
        String key = evidence.upload(JwtPayload.getCurrentUserPayload().getUserId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(Map.of("key", key)));
    }

    @Operation(summary = "Gửi báo cáo vi phạm nội dung hoặc người dùng", description = "Người dùng gửi đơn khiếu nại/báo cáo về một bài viết, bình luận hoặc tài khoản người dùng vi phạm tiêu chuẩn cộng đồng kèm bằng chứng hình ảnh (nếu có).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Gửi báo cáo thành công"),
        @ApiResponse(responseCode = "200", description = "Đã có báo cáo đang chờ của bạn"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ hoặc tự báo cáo chính mình (CANNOT_INTERACT_WITH_SELF)"),
            @ApiResponse(responseCode = "404", description = "Đối tượng bị báo cáo không tồn tại (USER_NOT_FOUND, POST_NOT_FOUND...)")
    })
    @PostMapping("/report")
    public ResponseEntity<GlobalResponse<ReportResponseDto>> createReport(@Valid @RequestBody CreateReportRequestDto request) {
        var result = reportService.createReport(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(GlobalResponse.success(result.report()));
    }
}
