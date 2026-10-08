package com.frozenheart.backend.modules.exam.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.exam.dto.ExamDetailResponse;
import com.frozenheart.backend.modules.exam.dto.ExamListResponse;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.GenerateExamRequest;
import com.frozenheart.backend.modules.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "20. Đề thi & Xuất Đề (Exams)", description = "Các API tự động tạo đề thi từ ngân hàng câu hỏi, xem chi tiết, quản lý danh sách đề và xuất file đề thi")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/exams")
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "Tự động sinh đề thi từ ngân hàng câu hỏi", description = "Dành cho giảng viên: bốc ngẫu nhiên câu hỏi đã được duyệt (APPROVED) theo môn học, số lượng yêu cầu, tỉ lệ độ khó và trọng số chủ đề, đồng thời gán vào các lớp học phần.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo đề thi thành công"),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ: Chưa chọn lớp học phần, không tìm thấy môn học hoặc ngân hàng đề trống"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Sinh viên không có quyền tạo đề thi")
    })
    @PostMapping("/generate")
    public ResponseEntity<GlobalResponse<ExamDetailResponse>> generateExam(
            @Valid @RequestBody GenerateExamRequest request
        ) {

        ExamDetailResponse response = examService.generateExam(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem chi tiết đề thi", description = "Lấy toàn bộ thông tin đề thi bao gồm danh sách câu hỏi theo thứ tự, các phương án đáp án, hình ảnh và thống kê tỷ lệ độ khó.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết đề thi thành công"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy đề thi")
    })
    @GetMapping("/{examId}")
    public ResponseEntity<GlobalResponse<ExamDetailResponse>> getExamDetail(
            @Parameter(description = "ID của đề thi", example = "5")
            @PathVariable Long examId) {

        ExamDetailResponse response = examService.getExamDetail(examId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách đề thi của giảng viên", description = "Truy vấn danh sách các đề thi do giảng viên hiện tại tạo ra, hỗ trợ lọc theo môn học và phân trang con trỏ cursor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách đề thi thành công")
    })
    @GetMapping("/my-exams")
    public ResponseEntity<GlobalResponse<ExamListResponse>> getMyExams(
            @Parameter(description = "ID đề thi cuối cùng của trang trước (cursor)", example = "20")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Số lượng bản ghi tối đa mỗi trang (mặc định 10, tối đa 20)", example = "10")
            @RequestParam(defaultValue = "10") Integer limit,

            @Parameter(description = "Lọc theo ID môn học", example = "1")
            @RequestParam(required = false, name = "subject_id") Long subjectId
            ) {

        ExamListResponse response = examService.getMyExams(subjectId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xuất file đề thi (PDF / Excel)", description = "Biên dịch và kết xuất file đề thi ra định dạng PDF hoặc Excel (tùy chọn kèm bảng đáp án), lưu trữ trên S3/MinIO và trả về presigned URL tải về.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xuất file đề thi thành công và trả về URL tải về"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy đề thi")
    })
    @GetMapping("/{examId}/export")
    public ResponseEntity<GlobalResponse<ExportResponse>> exportExam(
            @Parameter(description = "ID của đề thi", example = "5")
            @PathVariable Long examId,

            @Parameter(description = "Định dạng file xuất (pdf hoặc excel/xlsx)", example = "pdf")
            @RequestParam(defaultValue = "pdf") String format,

            @Parameter(description = "Có xuất kèm bảng đáp án đúng hay không", example = "false")
            @RequestParam(defaultValue = "false", name = "include_answer_key") Boolean includeAnswerKey,

            @Parameter(description = "Khổ giấy in", example = "A4")
            @RequestParam(defaultValue = "A4", name = "paper_size") String paperSize) {

        ExportResponse response = examService.exportExam(examId, format, includeAnswerKey, paperSize);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}

