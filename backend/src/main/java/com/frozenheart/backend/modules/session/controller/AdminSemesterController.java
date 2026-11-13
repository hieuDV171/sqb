package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.CreateSemesterRequest;
import com.frozenheart.backend.modules.session.dto.SemesterResponse;
import com.frozenheart.backend.modules.session.dto.UpdateSemesterStartDateRequest;
import com.frozenheart.backend.modules.session.service.SemesterManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "14. Quản trị Học kỳ (Semesters)", description = "Các API dành cho Quản trị viên quản lý vòng đời học kỳ: tạo học kỳ, kích hoạt, đóng kỳ, và xóa học kỳ")
@RestController
@RequestMapping("/admin/semesters")
@RequiredArgsConstructor
public class AdminSemesterController {

    private final SemesterManagementService semesterManagementService;

    @Operation(summary = "Tạo học kỳ mới", description = "Tạo một học kỳ mới ở trạng thái chưa kích hoạt (is_active = false, is_finalized = false).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo học kỳ mới thành công"),
            @ApiResponse(responseCode = "400", description = "Tên học kỳ không hợp lệ hoặc ngày khai giảng sai định dạng yyyy-MM-dd"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền quản trị viên (ADMIN)")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<SemesterResponse>> createSemester(
            @Valid @RequestBody CreateSemesterRequest request) {
        SemesterResponse response = semesterManagementService.createSemester(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách tất cả học kỳ", description = "Trả về toàn bộ danh sách các học kỳ trong hệ thống theo thứ tự mới nhất.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách học kỳ thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền quản trị viên (ADMIN)")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<List<SemesterResponse>>> getAllSemesters() {
        List<SemesterResponse> response = semesterManagementService.getAllSemesters();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Cập nhật ngày khai giảng", description = "Đặt hoặc sửa ngày khai giảng của một học kỳ hiện có, kể cả học kỳ đã chốt sổ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã cập nhật ngày khai giảng"),
            @ApiResponse(responseCode = "400", description = "Thiếu ngày khai giảng hoặc ngày sai định dạng yyyy-MM-dd"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền quản trị viên (ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy học kỳ (RESOURCE_NOT_FOUND)")
    })
    @PutMapping("/{semesterId}/start-date")
    public ResponseEntity<GlobalResponse<SemesterResponse>> updateSemesterStartDate(
            @Parameter(description = "ID học kỳ cần cập nhật", example = "1", required = true)
            @PathVariable Long semesterId,
            @Valid @RequestBody UpdateSemesterStartDateRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(
                semesterManagementService.updateSemesterStartDate(semesterId, request.startDate())));
    }

    @Operation(summary = "Kích hoạt học kỳ", description = "Kích hoạt học kỳ được chọn để phục vụ dạy học và đề xuất câu hỏi. Rào chắn: Chỉ cho phép kích hoạt nếu không có học kỳ nào khác đang mở và học kỳ này chưa từng bị chốt sổ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kích hoạt học kỳ thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Học kỳ đã chốt sổ hoặc không có quyền quản trị"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy học kỳ (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Đã có học kỳ khác hoạt động (ACTIVE_SEMESTER_CONFLICT)")
    })
    @PutMapping("/{semesterId}/activate")
    public ResponseEntity<GlobalResponse<SemesterResponse>> activateSemester(
            @Parameter(description = "ID học kỳ cần kích hoạt", example = "1", required = true)
            @PathVariable Long semesterId) {
        SemesterResponse response = semesterManagementService.activateSemester(semesterId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đóng toàn bộ học kỳ đang kích hoạt", description = "Hủy kích hoạt học kỳ đang mở. Rào chắn: Bắt buộc học kỳ đang active phải được Chốt Sổ (is_finalized = true) trước khi đóng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hủy kích hoạt toàn bộ học kỳ thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có học kỳ nào active hoặc học kỳ chưa được chốt sổ (ACTION_NOT_ALLOWED)")
    })
    @PutMapping("/deactive-all")
    public ResponseEntity<GlobalResponse<String>> deactivateAllSemesters() {
        semesterManagementService.deactivateAllSemesters();
        return ResponseEntity.ok(GlobalResponse.success("Đã hủy kích hoạt toàn bộ học kỳ"));
    }

    @Operation(summary = "Xóa học kỳ", description = "Xóa vĩnh viễn một học kỳ. Rào chắn: Không thể xóa học kỳ đang hoạt động hoặc học kỳ đã có lớp học phần trực thuộc.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã xóa học kỳ thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không thể xóa học kỳ đang hoạt động hoặc đã có lớp học phần trực thuộc (ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy học kỳ để xóa (RESOURCE_NOT_FOUND)")
    })
    @DeleteMapping("/{semesterId}")
    public ResponseEntity<GlobalResponse<String>> deleteSemester(
            @Parameter(description = "ID học kỳ cần xóa", example = "1", required = true)
            @PathVariable Long semesterId) {
        semesterManagementService.deleteSemester(semesterId);
        return ResponseEntity.ok(GlobalResponse.success("Đã xóa học kỳ thành công"));
    }
}
