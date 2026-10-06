package com.frozenheart.backend.modules.exam.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "19. Đề thi & Xuất Đề (Exams)", description = "Các API tự động tạo đề thi từ ngân hàng câu hỏi, xem chi tiết, quản lý danh sách đề và xuất file đề thi")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class QuestionExportController {

    private final DocumentExportService documentExportService;

    @Operation(summary = "Giảng viên xuất toàn bộ câu hỏi môn học", description = "Xuất danh sách tất cả câu hỏi của một môn học ra định dạng PDF hoặc Excel (có kèm đáp án và lời giải hoặc chỉ đề bài), hỗ trợ cơ chế lưu cache S3/MinIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xuất file câu hỏi thành công và trả về URL tải về"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy câu hỏi nào thuộc môn học này")
    })
    @GetMapping("/lecturer/questions/export")
    public ResponseEntity<GlobalResponse<ExportResponse>> exportQuestions(
            @Parameter(description = "ID môn học cần xuất câu hỏi", example = "10")
            @RequestParam(name = "subject_id") Long subjectId,

            @Parameter(description = "Định dạng file xuất (pdf hoặc excel/xlsx)", example = "pdf")
            @RequestParam(defaultValue = "pdf") String format,

            @Parameter(description = "Có xuất kèm đáp án đúng và lời giải hay không", example = "true")
            @RequestParam(defaultValue = "true", name = "include_answer") Boolean includeAnswer) {

        ExportResponse response = documentExportService.exportQuestions(subjectId, format, includeAnswer);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}

