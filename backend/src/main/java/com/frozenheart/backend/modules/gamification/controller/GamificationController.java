package com.frozenheart.backend.modules.gamification.controller;

import com.frozenheart.backend.core.annotation.Idempotent;
import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.gamification.dto.*;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "21. Gamification & Minigames", description = "Các API trò chơi dự đoán học thuật (Game 1, 2, 4, 5, 6), điểm danh hàng ngày và Bảng xếp hạng vinh danh")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @Operation(summary = "Lấy danh sách lớp học phần để dự đoán Game 1", description = "Lấy danh sách các lớp học phần sinh viên đang theo học cùng trạng thái và số lượng sinh viên đã cược nộp câu hỏi cho ngày mai.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách lớp học phần thành công")
    })
    @GetMapping("/games/prediction/game-1/classes")
    public ResponseEntity<GlobalResponse<List<MyCourseClassPredictionDto>>> getMyCourseClassesForGame1Prediction() {
        List<MyCourseClassPredictionDto> response = gamificationService.getMyCourseClassesForGame1Prediction();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đặt dự đoán Game 1: Số sinh viên nộp bài ngày mai", description = "Dự đoán số lượng sinh viên trong lớp học phần sẽ nộp câu hỏi vào ngày mai (chốt cược trước 22:00 mỗi ngày).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt dự đoán thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Bạn không tham gia lớp học phần này"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lớp học phần"),
            @ApiResponse(responseCode = "409", description = "Xung đột: Đã hết giờ đặt cược (sau 22:00) hoặc bạn đã đặt cược cho lớp này vào ngày mai rồi")
    })
    @PostMapping("/games/prediction/participants")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> predictGame1(
            @Valid @RequestBody Game1PredictionRequest request) {
        GamePredictionResponse response = gamificationService.predictGame1(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đặt dự đoán Game 2: Số câu hỏi LLM và Con người được duyệt", description = "Tác giả dự đoán số lượng câu hỏi do AI và con người tạo ra trong phiên của mình sẽ được Giảng viên duyệt vào ngân hàng đề.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt dự đoán thành công"),
            @ApiResponse(responseCode = "400", description = "Số câu hỏi đề xuất loại này bằng 0, không thể đặt cược"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên câu hỏi"),
            @ApiResponse(responseCode = "409", description = "Xung đột: Bạn đã đặt cược Game 2 cho phiên này rồi")
    })
    @PostMapping("/games/prediction/approved-questions")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> predictGame2(
            @Valid @RequestBody Game2PredictionRequest request) {
        GamePredictionResponse response = gamificationService.predictGame2(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đặt dự đoán Game 4: Tổng quy mô ngân hàng câu hỏi cuối kỳ", description = "Dự đoán tổng số lượng câu hỏi được phê duyệt vào ngân hàng câu hỏi của môn học khi kết thúc học kỳ. Top 3 người đoán chuẩn nhất nhận giải thưởng lớn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt dự đoán thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy môn học"),
            @ApiResponse(responseCode = "409", description = "Xung đột: Bạn đã đặt cược cho môn học này rồi")
    })
    @PostMapping("/games/prediction/bank-size")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> predictGame4(
            @Valid @RequestBody Game4PredictionRequest request) {
        GamePredictionResponse response = gamificationService.predictGame4(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy phiên Game 6 đang mở (Thứ 7 hàng tuần)", description = "Lấy danh sách 7 câu hỏi thử thách nhận diện nguồn gốc AI/Con người trong phiên minigame mở định kỳ vào Thứ 7.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin phiên Game 6 thành công"),
            @ApiResponse(responseCode = "404", description = "Hiện tại không có phiên Game 6 nào đang mở")
    })
    @GetMapping("/games/prediction/active-session")
    public ResponseEntity<GlobalResponse<Game6ActiveSessionResponse>> getActiveGame6Session() {
        Game6ActiveSessionResponse response = gamificationService.getActiveGame6Session();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Nộp dự đoán Game 6: Phán đoán câu hỏi do AI tạo", description = "Gửi danh sách các câu hỏi mà bạn nghi ngờ do AI/LLM sinh ra trong phiên Game 6.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nộp dự đoán thành công"),
            @ApiResponse(responseCode = "400", description = "Hết thời gian nộp hoặc bạn đã tham gia phiên này rồi"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên trò chơi")
    })
    @PostMapping("/games/prediction/submit")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> submitGame6(
            @Valid @RequestBody Game6SubmitRequest request) {
        GamePredictionResponse response = gamificationService.submitGame6(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy lịch sử các lượt dự đoán của tôi", description = "Tra cứu toàn bộ lịch sử các lượt tham gia minigame và kết quả dự đoán (trúng/trượt) của người dùng hiện tại.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy lịch sử dự đoán thành công")
    })
    @GetMapping("/games/my-predictions")
    public ResponseEntity<GlobalResponse<MyPredictionsResponse>> getMyPredictions(
            @Parameter(description = "ID lượt dự đoán cuối ở trang trước (cursor)", example = "1050")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Số lượng bản ghi tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(defaultValue = "10") Integer limit) {
        MyPredictionsResponse response = gamificationService.getMyPredictions(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Giảng viên thẩm định báo lỗi câu hỏi (Game 5)", description = "Dành cho Giảng viên: xác nhận câu hỏi có bị sai đề hoặc đáp án theo báo lỗi của sinh viên hay không. Nếu xác nhận lỗi, người báo được cộng điểm và tác giả bị trừ điểm.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thẩm định báo lỗi thành công"),
            @ApiResponse(responseCode = "400", description = "Đánh giá này không phải là báo lỗi"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đánh giá hoặc câu hỏi")
    })
    @PostMapping("/questions/{questionId}/ratings/{ratingUserId}/review-error")
    public ResponseEntity<GlobalResponse<String>> reviewErrorGame5(
            @Parameter(description = "ID người dùng gửi báo lỗi", example = "55")
            @PathVariable Long ratingUserId,

            @Parameter(description = "ID câu hỏi bị báo lỗi", example = "105")
            @PathVariable Long questionId,

            @Valid @RequestBody ReviewErrorRequest request) {
        gamificationService.reviewErrorGame5(ratingUserId, questionId, request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Chốt sổ tổng kết học kỳ (Admin)", description = "Quản trị viên chốt sổ học kỳ: khóa điểm, tổng kết Game 4, lưu trữ câu hỏi sang legacy, vinh danh thủ khoa và trao thưởng Xu/Huy hiệu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chốt sổ học kỳ thành công"),
            @ApiResponse(responseCode = "400", description = "Không có học kỳ đang kích hoạt hoặc học kỳ đã được chốt sổ trước đó")
    })
    @PostMapping("/admin/semesters/finalize-semester")
    public ResponseEntity<GlobalResponse<String>> finalizeSemester() {
        gamificationService.finalizeSemester();
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Xem Bảng xếp hạng vinh danh sinh viên", description = "Xem bảng xếp hạng tích lũy điểm trong học kỳ hoặc theo môn học kèm vị trí xếp hạng của chính mình.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy bảng xếp hạng thành công")
    })
    @GetMapping("/games/leaderboard")
    public ResponseEntity<GlobalResponse<LeaderboardResponse>> getLeaderboard(
            @Parameter(description = "Phạm vi bảng xếp hạng (SEMESTER hoặc SUBJECT)", example = "SEMESTER")
            @RequestParam(defaultValue = "SEMESTER") LeaderboardPeriod period,

            @Parameter(description = "ID môn học (bắt buộc nếu period = SUBJECT)", example = "10")
            @RequestParam(required = false, name = "subject_id") Long subjectId,

            @Parameter(description = "Cursor phân trang sau", example = "20")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Cursor phân trang trước", example = "1")
            @RequestParam(required = false) Long before,

            @Parameter(description = "Số lượng bản ghi tối đa (mặc định 20)", example = "20")
            @RequestParam(defaultValue = "20") Integer limit) {
        LeaderboardResponse response = gamificationService.getLeaderboard(period, subjectId, after, before, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy trạng thái điểm danh hôm nay của người dùng", description = "Kiểm tra xem người dùng hiện tại đã điểm danh hôm nay chưa và số ngày chuỗi liên tiếp (Streak).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy trạng thái điểm danh thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @GetMapping("/games/check-in")
    public ResponseEntity<GlobalResponse<CheckInStatusResponse>> getCheckInStatus() {
        CheckInStatusResponse response = gamificationService.getCheckInStatus();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Điểm danh chuyên cần hàng ngày", description = "Điểm danh hàng ngày nhận ngay +1 Xu 🪙 và tích lũy chuỗi ngày liên tiếp (Streak) để thăng hạng và nhận huy hiệu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Điểm danh thành công"),
            @ApiResponse(responseCode = "400", description = "Bạn đã điểm danh hôm nay rồi!"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @Idempotent(keyPrefix = "daily_checkin", expireSeconds = 60)
    @PostMapping("/games/check-in")
    public ResponseEntity<GlobalResponse<CheckInResponse>> checkInDaily() {
        CheckInResponse response = gamificationService.checkInDaily();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}

