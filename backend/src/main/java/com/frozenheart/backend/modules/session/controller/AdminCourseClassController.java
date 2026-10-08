package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.ExcelImportClassResult;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "15a. Quản trị Lớp học phần (Admin Course Classes)", description = "Các API dành riêng cho Quản trị viên quản lý, import danh sách lớp học phần và sinh viên từ file Excel")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/course-classes")
@RequiredArgsConstructor
public class AdminCourseClassController {

    private final CourseClassManagementService courseClassManagementService;

    @Operation(summary = "Import lớp học phần và sinh viên từ file Excel (Admin)", description = "Quy trình import 2 giai đoạn dành cho Quản trị viên: Tự động bóc tách mã lớp, mã môn, tự động tạo tài khoản sinh viên mới nếu chưa có trong hệ thống, và ghi danh vào lớp học phần tương ứng trong học kỳ đang mở.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import file Excel thành công"),
            @ApiResponse(responseCode = "400", description = "File không có dữ liệu sinh viên, thiếu mã lớp/mã môn, học kỳ trong Excel không khớp học kỳ active, hoặc không có học kỳ nào active (INVALID_PARAMETER_VALUE / NO_ACTIVE_SEMESTER)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Chỉ Quản trị viên (ADMIN) mới có quyền thực hiện"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy môn học theo mã môn bóc tách từ Excel (SUBJECT_NOT_FOUND)")
    })
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<ExcelImportClassResult>> importCourseClassFromExcel(
            @Parameter(description = "File bảng điểm danh sinh viên Excel (.xlsx, .xls)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID giảng viên phụ trách chỉ định (tùy chọn)", example = "2")
            @RequestParam(value = "lecturerId", required = false) Long lecturerId) {

        ExcelImportClassResult response = courseClassManagementService.importAndEnrollFromExcel(file, lecturerId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
