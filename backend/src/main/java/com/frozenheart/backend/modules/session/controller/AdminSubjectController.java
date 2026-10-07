package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.CreateSubjectRequest;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "14. Quản trị Môn học (Subjects)", description = "Các API dành cho Quản trị viên quản lý danh mục môn học trong hệ thống")
@RestController
@RequestMapping("/admin/subjects")
@RequiredArgsConstructor
public class AdminSubjectController {

    private final CourseClassManagementService courseClassManagementService;

    @Operation(summary = "Tạo môn học mới", description = "Tạo môn học mới trong hệ thống. Mã môn học là duy nhất và tự động chuẩn hóa viết hoa (VD: IT3180).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo môn học thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ (tên/mã môn trống hoặc vượt quá độ dài)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền quản trị viên (ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Mã môn học đã tồn tại trong hệ thống (SUBJECT_ALREADY_EXISTS)")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse response = courseClassManagementService.createSubject(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
