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
            @RequestParam(required = false) Long before,

            @Parameter(description = "Số lượng phiên tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false) Integer limit,

            @Parameter(description = "Lọc theo ID lớp học phần được phân công", example = "100")
            @RequestParam(required = false, name = "course_class_id") Long courseClassId,

            @Parameter(description = "Tiêu chí sắp xếp", example = "created_at")
            @RequestParam(required = false, name = "sort_by") String sortBy,
            @RequestParam(name = "q", required = false) String keyword) {

        ReviewableSessionsResponse response = questionReviewService
                .getReviewableSessions(after, before, limit, courseClassId, sortBy, keyword);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem chi tiết phiên đề xuất để duyệt", description = "Mở phiên PENDING sẽ chuyển phiên sang REVIEWING. Phản hồi trả trạng thái phiên và các cảnh báo trùng lặp. Phiên RESOLVED chỉ đọc.")
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

    @Operation(summary = "Ghi quyết định phê duyệt danh sách câu hỏi", description = "Giảng viên có thể đổi quyết định đến khi chốt phiên. Chỉ sau khi chốt, câu được duyệt mới được dùng để tạo đề và tính điểm thưởng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã ghi quyết định phê duyệt, điểm cộng bằng 0"),
            @ApiResponse(responseCode = "400", description = "Danh sách câu hỏi trống hoặc không hợp lệ"),
            @ApiResponse(responseCode = "409", description = "Phiên đã chốt (SESSION_REVIEW_FINALIZED)")
    })
    @PostMapping("/sessions/approve")
    public ResponseEntity<GlobalResponse<ApproveQuestionsResponse>> approveQuestions(
            @Valid @RequestBody ApproveQuestionsRequest request) {

        ApproveQuestionsResponse response = questionReviewService.approveQuestions(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Từ chối danh sách câu hỏi", description = "Ghi lý do từ chối. Có thể đổi quyết định cho tới khi chốt phiên.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Từ chối câu hỏi thành công"),
            @ApiResponse(responseCode = "400", description = "Danh sách câu hỏi trống hoặc không hợp lệ"),
            @ApiResponse(responseCode = "409", description = "Phiên đã chốt (SESSION_REVIEW_FINALIZED)")
    })
    @PostMapping("/sessions/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectQuestions(
            @Valid @RequestBody RejectQuestionsRequest request) {

        questionReviewService.rejectQuestions(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Giảng viên chỉnh sửa câu hỏi", description = "Chỉnh sửa nội dung, đáp án, giải thích và tùy chọn ghi quyết định phê duyệt trước khi chốt phiên.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chỉnh sửa câu hỏi thành công"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Chỉ giảng viên mới có quyền sửa câu hỏi"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi"),
            @ApiResponse(responseCode = "409", description = "Phiên đã chốt (SESSION_REVIEW_FINALIZED)")
    })
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<GlobalResponse<EditQuestionResponse>> editQuestion(
            @Parameter(description = "ID câu hỏi cần sửa", example = "105")
            @PathVariable Long questionId,
            @RequestBody EditQuestionRequest request) {

        EditQuestionResponse response = questionReviewService.editQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Chốt hoàn tất duyệt phiên đề xuất", description = "Khóa kết quả và chuyển phiên sang RESOLVED. Các câu còn PENDING bị từ chối. Ghi nhận điểm và thông báo nếu có câu được duyệt. Gọi lại với phiên đã chốt trả thành công mà không ghi nhận lặp.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chốt hoàn tất duyệt phiên thành công"),
            @ApiResponse(responseCode = "400", description = "Phiên chưa có câu hỏi"),
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
