package com.frozenheart.backend.modules.report.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.dto.ReportResponseDto;
import com.frozenheart.backend.modules.report.service.ReportService;
import com.frozenheart.backend.modules.report.service.ReportEvidenceStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

    @Operation(summary = "Tải một ảnh bằng chứng vào kho riêng tư", description = "Người dùng đăng nhập gửi multipart field file, ảnh PNG/JPEG/GIF/WebP hợp lệ, tối đa 5 MiB. Nhận object key thuộc chính tài khoản để gửi evidence_keys khi tạo báo cáo trong 23 giờ; tối đa một ảnh/báo cáo, không gửi đồng thời evidence_keys và evidence_urls. Đây không phải URL xem ảnh công khai. Có giới hạn số lần tải và quota R2.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ảnh staging đã tải; dùng data.key trong evidence_keys", content = @Content(examples = @ExampleObject(value = "{\"code\":\"1000\",\"message\":\"Thành công\",\"data\":{\"key\":\"report-evidence/7/550e8400-e29b-41d4-a716-446655440000\"}}"))),
            @ApiResponse(responseCode = "400", description = "Sai định dạng/nội dung ảnh hoặc dung lượng"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc AT hết hạn"),
            @ApiResponse(responseCode = "429", description = "Quá số lần tải bằng chứng cho phép"),
            @ApiResponse(responseCode = "507", description = "Vượt quota lưu trữ R2"),
            @ApiResponse(responseCode = "503", description = "Kho lưu trữ hoặc quota chưa khả dụng")
    })
    @PostMapping(value = "/report/evidence", consumes = "multipart/form-data")
    public ResponseEntity<GlobalResponse<Map<String, String>>> uploadEvidence(
            @Parameter(description = "Binary ảnh bằng chứng, >0 và <=5 MiB, đúng MIME và chữ ký tệp") @RequestPart("file") MultipartFile file) {
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
