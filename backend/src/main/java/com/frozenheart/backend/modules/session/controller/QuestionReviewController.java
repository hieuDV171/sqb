package com.frozenheart.backend.modules.session.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.EditQuestionRequest;
import com.frozenheart.backend.modules.session.dto.EditQuestionResponse;
import com.frozenheart.backend.modules.session.dto.ReviewableSessionsResponse;
import com.frozenheart.backend.modules.session.dto.RejectQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.SessionDetailReviewResponse;
import com.frozenheart.backend.modules.session.service.QuestionReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "18. Giảng viên Duyệt Câu hỏi (Question Review)", description = "Các API dành cho Giảng viên / Quản trị viên thẩm định, chỉnh sửa, phê duyệt hoặc từ chối câu hỏi đề xuất")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class QuestionReviewController {

    private final QuestionReviewService questionReviewService;

    @Operation(summary = "Lấy danh sách phiên có thể kiểm duyệt", description = "Lấy các phiên PENDING/REVIEWING thuộc đúng lớp học phần được phân công cho giảng viên hiện tại; Admin có thể xem toàn bộ. Hỗ trợ phân trang con trỏ và lọc theo lớp học phần.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách phiên có thể kiểm duyệt thành công")
    })
    @GetMapping("/sessions/reviewable")
    public ResponseEntity<GlobalResponse<ReviewableSessionsResponse>> getReviewableSessions(
            @Parameter(description = "ID phiên cuối cùng của trang trước (cursor)", example = "50")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Số lượng phiên tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false) Integer limit,

            @Parameter(description = "Lọc theo ID lớp học phần được phân công", example = "100")
            @RequestParam(required = false, name = "course_class_id") Long courseClassId,

            @Parameter(description = "Tiêu chí sắp xếp", example = "created_at")
            @RequestParam(required = false, name = "sort_by") String sortBy) {

        ReviewableSessionsResponse response = questionReviewService
                .getReviewableSessions(after, limit, courseClassId, sortBy);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem chi tiết phiên đề xuất để duyệt", description = "Giảng viên mở xem toàn bộ câu hỏi trong phiên để duyệt. Phiên sẽ chuyển sang trạng thái REVIEWING và hệ thống trả về cảnh báo trùng lặp tự động (nếu có).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết phiên để duyệt thành công"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Sinh viên không có quyền duyệt câu hỏi"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên đề xuất")
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<GlobalResponse<SessionDetailReviewResponse>> getSessionDetailForReview(
            @Parameter(description = "ID của phiên đề xuất", example = "12")
            @PathVariable Long sessionId) {

        SessionDetailReviewResponse response = questionReviewService.getSessionDetailForReview(sessionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Phê duyệt danh sách câu hỏi", description = "Phê duyệt các câu hỏi đạt chuẩn vào Ngân hàng câu hỏi chính thức. Tự động thưởng điểm cho tác giả và tự động hoàn tất phiên nếu tất cả câu hỏi đã được xử lý.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phê duyệt câu hỏi thành công và trả về số điểm thưởng"),
            @ApiResponse(responseCode = "400", description = "Danh sách câu hỏi trống hoặc không hợp lệ")
    })
    @PostMapping("/sessions/approve")
    public ResponseEntity<GlobalResponse<ApproveQuestionsResponse>> approveQuestions(
            @Valid @RequestBody ApproveQuestionsRequest request) {

        ApproveQuestionsResponse response = questionReviewService.approveQuestions(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Từ chối danh sách câu hỏi", description = "Từ chối các câu hỏi không đạt chất lượng hoặc trùng lặp. Tự động cập nhật trạng thái phiên nếu tất cả câu hỏi đã được xử lý.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Từ chối câu hỏi thành công"),
            @ApiResponse(responseCode = "400", description = "Danh sách câu hỏi trống hoặc không hợp lệ")
    })
    @PostMapping("/sessions/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectQuestions(
            @Valid @RequestBody RejectQuestionsRequest request) {

        questionReviewService.rejectQuestions(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Giảng viên chỉnh sửa câu hỏi", description = "Cho phép giảng viên chỉnh sửa nội dung, đáp án, giải thích của câu hỏi và tùy chọn tự động duyệt ngay vào ngân hàng đề.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chỉnh sửa câu hỏi thành công"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Chỉ giảng viên mới có quyền sửa câu hỏi"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi")
    })
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<GlobalResponse<EditQuestionResponse>> editQuestion(
            @Parameter(description = "ID câu hỏi cần sửa", example = "105")
            @PathVariable Long questionId,
            @RequestBody EditQuestionRequest request) {

        EditQuestionResponse response = questionReviewService.editQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Chốt hoàn tất duyệt phiên đề xuất", description = "Giảng viên chốt kết thúc quá trình duyệt phiên. Các câu hỏi chưa được duyệt (còn PENDING) sẽ tự động bị từ chối (REJECTED), phiên chuyển sang RESOLVED và gửi thông báo cho tác giả.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chốt hoàn tất duyệt phiên thành công"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Chỉ giảng viên mới có quyền hoàn tất duyệt"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên đề xuất")
    })
    @PostMapping("/sessions/{sessionId}/complete-review")
    public ResponseEntity<GlobalResponse<Void>> completeSessionReview(
            @Parameter(description = "ID phiên đề xuất", example = "12")
            @PathVariable Long sessionId) {
        questionReviewService.completeSessionReview(sessionId);
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
