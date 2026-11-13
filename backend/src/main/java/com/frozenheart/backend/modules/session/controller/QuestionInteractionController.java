package com.frozenheart.backend.modules.session.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionRequest;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionResponse;
import com.frozenheart.backend.modules.session.dto.QuestionRatingsResponse;
import com.frozenheart.backend.modules.session.dto.QuestionStatisticsResponse;
import com.frozenheart.backend.modules.session.dto.RateQuestionRequest;
import com.frozenheart.backend.modules.session.dto.RateQuestionResponse;
import com.frozenheart.backend.modules.session.dto.ReportQuestionRequest;
import com.frozenheart.backend.modules.session.dto.SessionQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.SubmissionsResponse;
import com.frozenheart.backend.modules.session.service.QuestionInteractionService;
import com.frozenheart.backend.modules.session.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "19. Luyện tập & Đánh giá Câu hỏi (Question Interaction)", description = "Các API sinh viên luyện tập, nộp câu trả lời, xem thống kê, chấm điểm và đánh giá chất lượng câu hỏi")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class QuestionInteractionController {

    private final QuestionInteractionService questionInteractionService;
    private final SessionService sessionService;

    @Operation(summary = "Gửi hoặc cập nhật báo lỗi câu hỏi",
            description = "Lưu một báo lỗi theo cặp người dùng và câu hỏi thuộc phiên đã xét duyệt. Gửi lại sẽ cập nhật nội dung báo lỗi.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lưu báo lỗi thành công"),
            @ApiResponse(responseCode = "400", description = "Mô tả lỗi bị bỏ trống hoặc dài quá 500 ký tự"),
            @ApiResponse(responseCode = "401", description = "Phiên đăng nhập thiếu hoặc hết hạn"),
            @ApiResponse(responseCode = "403", description = "Câu hỏi thuộc phiên chưa xét duyệt hoặc người gọi không có quyền"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi")
    })
    @PutMapping("/questions/{questionId}/report")
    public ResponseEntity<GlobalResponse<Void>> reportQuestion(
            @Parameter(description = "ID câu hỏi cần báo lỗi", example = "105")
            @PathVariable Long questionId,
            @Valid @RequestBody ReportQuestionRequest request) {
        questionInteractionService.reportQuestion(questionId, request.description());
        return ResponseEntity.ok(GlobalResponse.success(null));
    }

    @Deprecated
    @Operation(summary = "Lấy danh sách phiên đề xuất của chính mình theo ID người dùng",
            deprecated = true,
            description = "API tương thích cho client cũ, chỉ truy cập được khi ID trùng với userId. Dùng GET /sessions/my-submissions để xem phiên của mình hoặc GET /sessions/practice-catalog để tìm bộ câu hỏi luyện tập theo môn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách phiên thành công"),
            @ApiResponse(responseCode = "403", description = "Không được xem danh sách phiên của tài khoản khác")
    })
    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<GlobalResponse<SubmissionsResponse>> getUserProposedSessions(
            @Parameter(description = "ID của người dùng cần tra cứu", example = "42")
            @PathVariable Long userId,

            @Parameter(description = "ID phiên cuối của trang trước (cursor)", example = "30")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Số lượng phiên tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false) Integer limit,

            @Parameter(description = "Lọc theo ID môn học", example = "1")
            @RequestParam(required = false, name = "subject_id") Long subjectId,

            @Parameter(description = "Lọc theo trạng thái phiên", example = "RESOLVED")
            @RequestParam(required = false) SessionStatus status) {

        SubmissionsResponse response = sessionService.getUserSubmissions(userId, after, limit, subjectId, status);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách câu hỏi trong phiên để luyện tập", description = "Lấy toàn bộ câu hỏi trong một phiên phục vụ làm bài luyện tập. Đáp án đúng và lời giải sẽ bị ẩn nếu người dùng chưa hoàn thành câu hỏi đó.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách câu hỏi thành công"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Phiên chưa được phê duyệt và bạn không phải là tác giả"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên câu hỏi")
    })
    @GetMapping("/sessions/{sessionId}/questions")
    public ResponseEntity<GlobalResponse<SessionQuestionsResponse>> getSessionQuestions(
            @Parameter(description = "ID của phiên câu hỏi", example = "12")
            @PathVariable Long sessionId) {

        SessionQuestionsResponse response = questionInteractionService.getSessionQuestions(sessionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Nộp câu trả lời cho câu hỏi", description = "Sinh viên gửi đáp án đã chọn. Hệ thống lập tức chấm điểm, lưu lại lịch sử làm bài và trả về kết quả đúng/sai cùng đáp án đúng và lời giải chi tiết.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chấm điểm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu trả lời không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi"),
            @ApiResponse(responseCode = "409", description = "Xung đột: Bạn đã trả lời câu hỏi này rồi, không thể nộp lại"),
            @ApiResponse(responseCode = "412", description = "Điều kiện tiên quyết không thỏa mãn: Nội dung câu hỏi đã bị sửa đổi kể từ lúc tải về (Stale data)")
    })
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<GlobalResponse<AnswerQuestionResponse>> answerQuestion(
            @Parameter(description = "ID câu hỏi", example = "105")
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerQuestionRequest request) {

        AnswerQuestionResponse response = questionInteractionService.answerQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem thống kê tương tác của câu hỏi", description = "Xem tổng số lượt làm bài, tỉ lệ làm đúng, phân bố lựa chọn của các phương án (A, B, C, D) và thống kê đánh giá sao từ cộng đồng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi")
    })
    @GetMapping("/questions/{questionId}/statistics")
    public ResponseEntity<GlobalResponse<QuestionStatisticsResponse>> getQuestionStatistics(
            @Parameter(description = "ID câu hỏi", example = "105")
            @PathVariable Long questionId) {

        QuestionStatisticsResponse response = questionInteractionService.getQuestionStatistics(questionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đánh giá chất lượng câu hỏi",
            description = "Lưu hoặc cập nhật đánh giá sao theo thang điểm từ 0 đến 4, tương ứng từ 1 đến 5 sao. Có thể gửi kèm nhận xét.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đánh giá câu hỏi thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đánh giá không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Câu hỏi thuộc phiên chưa xét duyệt hoặc người gọi không có quyền"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi")
    })
    @PostMapping("/questions/{questionId}/rate")
    public ResponseEntity<GlobalResponse<RateQuestionResponse>> rateQuestion(
            @Parameter(description = "ID câu hỏi", example = "105")
            @PathVariable Long questionId,
            @Valid @RequestBody RateQuestionRequest request) {

        RateQuestionResponse response = questionInteractionService.rateQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách đánh giá của câu hỏi", description = "Truy vấn danh sách các lượt đánh giá và nhận xét về câu hỏi từ những người học khác, hỗ trợ phân trang con trỏ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách đánh giá thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy câu hỏi")
    })
    @GetMapping("/questions/{questionId}/ratings")
    public ResponseEntity<GlobalResponse<QuestionRatingsResponse>> getQuestionRatings(
            @Parameter(description = "ID câu hỏi", example = "105")
            @PathVariable Long questionId,

            @Parameter(description = "ID người đánh giá cuối ở trang trước (cursor)", example = "30")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Số lượng bản ghi tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false) Integer limit) {

        QuestionRatingsResponse response = questionInteractionService.getQuestionRatings(questionId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
