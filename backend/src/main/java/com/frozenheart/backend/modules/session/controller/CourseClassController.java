package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.*;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
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

@Tag(name = "16a. Quản lý Lớp học phần (Course Classes)", description = "Các API dành cho Giảng viên và Quản trị viên quản lý lớp học phần, gán sinh viên, import danh sách lớp từ Excel và tra cứu thông tin lớp")
@RestController
@RequestMapping("/lecturer/course-classes")
@RequiredArgsConstructor
public class CourseClassController {

    private final CourseClassManagementService courseClassManagementService;

    @Operation(summary = "Tạo lớp học phần mới", description = "Tạo một lớp học phần mới trong học kỳ hiện tại hoặc chỉ định. Giảng viên phụ trách mặc định là người tạo hoặc chỉ định bởi Admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo lớp học phần thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ hoặc không có học kỳ nào đang mở (NO_ACTIVE_SEMESTER)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy giảng viên (USER_NOT_FOUND) hoặc môn học (SUBJECT_NOT_FOUND) hoặc học kỳ (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Lớp học phần đã tồn tại trong học kỳ (COURSE_CLASS_ALREADY_EXISTS)")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<CourseClassResponse>> createCourseClass(
            @Valid @RequestBody CreateCourseClassRequest request) {
        CourseClassResponse response = courseClassManagementService.createCourseClass(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách lớp học phần trong học kỳ hiện tại", description = "Trả về toàn bộ danh sách lớp học phần của học kỳ đang mở (active semester).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách lớp học phần thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<List<CourseClassResponse>>> getCourseClasses() {
        List<CourseClassResponse> response = courseClassManagementService.getCourseClassesByActiveSemester();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách lớp học phần do tôi phụ trách", description = "Trả về danh sách các lớp học phần trong học kỳ hiện tại mà Giảng viên đăng nhập đang phụ trách.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách lớp của tôi thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/my-classes")
    public ResponseEntity<GlobalResponse<List<CourseClassResponse>>> getMyCourseClasses() {
        List<CourseClassResponse> response = courseClassManagementService.getMyCourseClasses();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy chi tiết lớp học phần", description = "Trả về thông tin chi tiết một lớp học phần kèm môn học, học kỳ, giảng viên phụ trách và tổng số sinh viên.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin chi tiết lớp thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lớp học phần (COURSE_CLASS_NOT_FOUND)")
    })
    @GetMapping("/{courseClassId}")
    public ResponseEntity<GlobalResponse<CourseClassResponse>> getCourseClassDetail(
            @Parameter(description = "ID lớp học phần", example = "100", required = true)
            @PathVariable Long courseClassId) {
        CourseClassResponse response = courseClassManagementService.getCourseClassDetail(courseClassId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Gán sinh viên vào lớp học phần", description = "Thêm sinh viên vào lớp học phần bằng danh sách mã sinh viên (MSSV) hoặc ID người dùng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gán sinh viên vào lớp thành công (trả về số lượng đã gán, đã có, không tìm thấy)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Bạn không phụ trách lớp học phần này (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lớp học phần (COURSE_CLASS_NOT_FOUND)")
    })
    @PostMapping("/{courseClassId}/students")
    public ResponseEntity<GlobalResponse<AssignStudentsResponse>> assignStudents(
            @Parameter(description = "ID lớp học phần", example = "100", required = true)
            @PathVariable Long courseClassId,
            @RequestBody AssignStudentsRequest request) {
        AssignStudentsResponse response = courseClassManagementService.assignStudentsToClass(courseClassId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách sinh viên trong lớp học phần", description = "Trả về danh sách tất cả sinh viên đã ghi danh vào lớp kèm thông tin chi tiết (MSSV, họ tên, email, lớp sinh hoạt, viện/khoa, ngày sinh).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách sinh viên thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/{courseClassId}/students")
    public ResponseEntity<GlobalResponse<List<ClassStudentResponse>>> getClassStudents(
            @Parameter(description = "ID lớp học phần", example = "100", required = true)
            @PathVariable Long courseClassId) {
        List<ClassStudentResponse> response = courseClassManagementService.getClassStudents(courseClassId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách tất cả giảng viên đang hoạt động", description = "Phục vụ cho dropdown chọn giảng viên phụ trách lớp học phần khi tạo lớp.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách giảng viên thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/lecturers")
    public ResponseEntity<GlobalResponse<List<LecturerSummaryResponse>>> getLecturers() {
        List<LecturerSummaryResponse> response = courseClassManagementService.getAllActiveLecturers();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}

