package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "14b. Danh mục Môn học (Subjects Catalog)", description = "Các API tra cứu danh mục môn học chung dành cho Sinh viên, Giảng viên và Quản trị viên")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final CourseClassManagementService courseClassManagementService;

    @Operation(summary = "Lấy danh sách tất cả môn học", description = "Trả về danh mục tất cả môn học trong hệ thống được sắp xếp theo thứ tự bảng chữ cái A-Z. Dành cho mọi người dùng đã đăng nhập (Sinh viên, Giảng viên, Admin).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách môn học thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<List<SubjectResponse>>> getAllSubjects() {
        List<SubjectResponse> response = courseClassManagementService.getAllSubjects();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
