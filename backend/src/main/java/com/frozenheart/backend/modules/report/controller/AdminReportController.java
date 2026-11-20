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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Báo cáo vi phạm · Quản trị", description = "Chỉ ADMIN, qua hostname quản trị/Cloudflare Access theo triển khai SQB. Duyệt báo cáo, ảnh bằng chứng riêng tư, áp dụng và hoàn tác biện pháp; client nghiệp vụ thông thường không gọi nhóm này.")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công; dữ liệu trong GlobalResponse.data"),
        @ApiResponse(responseCode = "400", description = "Bộ lọc, quyết định, lý do hoặc thời hạn đình chỉ không hợp lệ"),
        @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn Bearer AT"),
        @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN hoặc bị lớp Access từ chối"),
        @ApiResponse(responseCode = "404", description = "Báo cáo/biện pháp/bằng chứng không tồn tại"),
        @ApiResponse(responseCode = "409", description = "REPORT_CONFLICT: version hoặc trạng thái đối tượng đã thay đổi; tải lại chi tiết")
})
public class AdminReportController {
    private final AdminReportService reports;

    @Operation(summary = "Lọc hàng đợi báo cáo vi phạm", description = "ADMIN đọc báo cáo mới trước theo ID giảm dần, phân trang after/limit. Có thể kết hợp trạng thái, loại đối tượng và loại vi phạm. Trả snapshot tóm tắt, tình trạng đối tượng hiện tại và số báo cáo PENDING cùng đối tượng; không tự đổi trạng thái khi xem.")
    @GetMapping("/reports")
    public GlobalResponse<CursorResponse<Summary>> queue(
            @Parameter(description = "PENDING, RESOLVED hoặc ALL; mặc định PENDING", example = "PENDING") @RequestParam(defaultValue = "PENDING") String status,
            @Parameter(description = "Lọc loại đối tượng; bỏ trống để lấy mọi loại", example = "POST") @RequestParam(name = "target_type", required = false) ReportTargetType targetType,
            @Parameter(description = "Lọc loại vi phạm; bỏ trống để không lọc", example = "SPAM") @RequestParam(name = "violation_type", required = false) ViolationType violationType,
            @Parameter(description = "ID cursor từ pagination.after của trang trước; trang đầu bỏ trống", example = "120") @RequestParam(required = false) Long after,
            @Parameter(description = "Số mục, mặc định 20; server giới hạn trong 1..50", example = "20") @RequestParam(required = false) Integer limit) {
        ReportStatus filter = switch (status) {
            case "ALL" -> null;
            case "PENDING" -> ReportStatus.PENDING;
            case "RESOLVED" -> ReportStatus.RESOLVED;
            default -> throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
        };
        return GlobalResponse.success(reports.queue(filter, targetType, violationType, after, limit));
    }

    @Operation(summary = "Xem chi tiết báo cáo và các quyết định hợp lệ", description = "Trả snapshot lúc báo cáo, snapshot hiện tại, người báo cáo/duyệt, version để xử lý đồng thời, allowed_actions và lịch sử biện pháp. Chỉ gửi resolve với action đang có trong allowed_actions; không dùng ID đối tượng thay ID báo cáo.")
    @GetMapping("/reports/{id}")
    public GlobalResponse<Detail> detail(@Parameter(description = "ID báo cáo", example = "120") @PathVariable Long id) {
        return GlobalResponse.success(reports.detail(id));
    }

    @Operation(summary = "Xem các báo cáo khác cùng đối tượng", description = "Đọc các đơn cùng target_type/target_id, loại trừ đơn đang xem; phân trang ID giảm dần. Không xử lý hàng loạt các báo cáo liên quan khi mở danh sách.")
    @GetMapping("/reports/{id}/related")
    public GlobalResponse<CursorResponse<Summary>> related(
            @Parameter(description = "ID báo cáo gốc", example = "120") @PathVariable Long id,
            @Parameter(description = "Cursor pagination.after; trang đầu bỏ trống", example = "100") @RequestParam(required = false) Long after,
            @Parameter(description = "Số mục, mặc định 20; giới hạn 1..50", example = "20") @RequestParam(required = false) Integer limit) {
        return GlobalResponse.success(reports.related(id, after, limit));
    }

    @Operation(summary = "Cấp URL ngắn hạn xem bằng chứng riêng tư", description = "Chỉ ADMIN đọc ảnh đã gắn vào báo cáo. URL GET có chữ ký sống 120 giây, response no-store; không lưu URL dài hạn hoặc công khai. Hết hạn gọi lại API này. Báo cáo không có private evidence trả 404.")
    @GetMapping("/reports/{id}/evidence")
    public ResponseEntity<GlobalResponse<Evidence>> evidence(@Parameter(description = "ID báo cáo có ảnh bằng chứng", example = "120") @PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(GlobalResponse.success(reports.evidence(id)));
    }

    @Operation(summary = "Đóng báo cáo và áp dụng quyết định kiểm duyệt", description = "Gửi version mới nhất từ Detail cùng action/reason. Chỉ PENDING được xử lý; version/trạng thái đã thay đổi trả 409. HIDE_CONTENT cho POST/COMMENT; WARN_ACCOUNT/SUSPEND_ACCOUNT cho USER, không xử phạt ADMIN. SUSPEND_ACCOUNT yêu cầu suspension_days thuộc 1/7/30; action khác phải bỏ trường này. Trả Detail mới, ghi lịch sử và thông báo người liên quan.")
    @PostMapping("/reports/{id}/resolve")
    public GlobalResponse<Detail> resolve(@Parameter(description = "ID báo cáo PENDING cần xử lý", example = "120") @PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return GlobalResponse.success(reports.resolve(id, request));
    }

    @Operation(summary = "Hoàn tác biện pháp ẩn nội dung hoặc đình chỉ", description = "ID ở đây là moderation action, không phải report ID. Chỉ hoàn tác HIDE_CONTENT/SUSPEND_ACCOUNT còn đang áp dụng; thay đổi đồng thời trả 409. Ghi một biện pháp khôi phục mới và gửi thông báo, không xóa lịch sử cũ. WARN_ACCOUNT không hỗ trợ hoàn tác bằng API này.")
    @PostMapping("/moderation-actions/{id}/reverse")
    public GlobalResponse<Detail> reverse(@Parameter(description = "ID biện pháp từ active_action_id/history cần hoàn tác", example = "42") @PathVariable Long id, @Valid @RequestBody ReverseRequest request) {
        return GlobalResponse.success(reports.reverse(id, request));
    }
}
