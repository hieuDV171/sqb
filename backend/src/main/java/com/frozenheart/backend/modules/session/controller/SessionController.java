package com.frozenheart.backend.modules.session.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.modules.session.dto.MySubmissionDetailResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.dto.UpdateSubmissionSessionRequest;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.dto.SubmissionsResponse;
import com.frozenheart.backend.modules.session.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "16. Phiên Đề xuất Câu hỏi (Sessions)", description = "Quản lý tạo mới, tra cứu, chỉnh sửa và xóa phiên đề xuất câu hỏi trắc nghiệm của sinh viên/người dùng")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "Đề xuất phiên câu hỏi mới", description = "Sinh viên/người dùng đề xuất danh sách câu hỏi trắc nghiệm kèm đáp án và hình ảnh cho một môn học đang theo học.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đề xuất phiên câu hỏi thành công",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc môn học không tồn tại",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Sinh viên chưa đăng ký lớp học phần thuộc môn này",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng hoặc tệp ảnh đính kèm đã hết hạn lưu tạm thời",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class)))
    })
    @PostMapping("/propose")
    public ResponseEntity<GlobalResponse<ProposeSessionResponse>> proposeSession(
            @Valid @RequestBody ProposeSessionRequest request) {

        ProposeSessionResponse response = sessionService.proposeSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách môn học người dùng đang tham gia", description = "Trả về danh sách các môn học mà sinh viên đang theo học dựa trên các lớp học phần đã tham gia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách môn học thành công",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class)))
    })
    @GetMapping("/my-enrolled-subjects")
    public ResponseEntity<GlobalResponse<List<SubjectResponse>>> getMyEnrolledSubjects() {
        List<SubjectResponse> response = sessionService.getMyEnrolledSubjects();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách phiên đề xuất của chính mình", description = "Truy vấn danh sách các phiên đề xuất câu hỏi do chính người dùng hiện tại nộp, hỗ trợ phân trang dạng con trỏ và lọc theo trạng thái/môn học.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách phiên đề xuất thành công",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class)))
    })
    @GetMapping("/my-submissions")
    public ResponseEntity<GlobalResponse<SubmissionsResponse>> getMySubmissions(
            @Parameter(description = "ID của phiên cuối cùng ở trang trước (cursor để phân trang)", example = "45")
            @RequestParam(required = false) Long after,

            @Parameter(description = "Số lượng phiên tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false) Integer limit,

            @Parameter(description = "Lọc theo ID môn học", example = "1")
            @RequestParam(required = false, name = "subject_id") Long subjectId,

            @Parameter(description = "Lọc theo trạng thái phiên đề xuất (PENDING: Chờ duyệt, REVIEWING: Đang duyệt, RESOLVED: Đã xử lý)", example = "PENDING")
            @RequestParam(required = false) SessionStatus status) {

        SubmissionsResponse response = sessionService.getMySubmissions(after, limit, subjectId, status);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem chi tiết phiên đề xuất của chính mình", description = "Xem thông tin chi tiết phiên đề xuất bao gồm tất cả câu hỏi, danh sách đáp án, ảnh minh họa, điểm tự tin và thông tin giảng viên duyệt (nếu đã duyệt).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết phiên đề xuất thành công",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Phiên đề xuất không thuộc quyền sở hữu của người dùng hiện tại",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên đề xuất",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class)))
    })
    @GetMapping("/my-submissions/{sessionId}")
    public ResponseEntity<GlobalResponse<MySubmissionDetailResponse>> getMySubmissionDetail(
            @Parameter(description = "ID của phiên đề xuất", example = "12")
            @PathVariable Long sessionId) {

        MySubmissionDetailResponse response = sessionService.getMySubmissionDetail(sessionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Cập nhật phiên đề xuất của chính mình", description = "Cho phép tác giả cập nhật thông tin phiên hoặc danh sách câu hỏi/đáp án. Chỉ áp dụng cho phiên đang ở trạng thái chờ duyệt (PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật phiên đề xuất thành công",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc môn học không tồn tại",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Không phải chủ sở hữu hoặc phiên đã được xử lý/duyệt",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên đề xuất",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class)))
    })
    @PutMapping("/my-submissions/{sessionId}")
    public ResponseEntity<GlobalResponse<Void>> updateMySubmissionSession(
            @Parameter(description = "ID của phiên đề xuất", example = "12")
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSubmissionSessionRequest request) {

        sessionService.updateMySubmissionSession(sessionId, request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Xóa phiên đề xuất của chính mình", description = "Cho phép tác giả hủy và xóa phiên đề xuất kèm toàn bộ câu hỏi và hình ảnh liên quan. Chỉ áp dụng khi phiên đang ở trạng thái chờ duyệt (PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa phiên đề xuất thành công",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "403", description = "Từ chối truy cập: Không phải chủ sở hữu hoặc phiên đã được xử lý/duyệt",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiên đề xuất",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class)))
    })
    @DeleteMapping("/my-submissions/{sessionId}")
    public ResponseEntity<GlobalResponse<Void>> deleteMySubmissionSession(
            @Parameter(description = "ID của phiên đề xuất", example = "12")
            @PathVariable Long sessionId) {

        sessionService.deleteMySubmissionSession(sessionId);
        return ResponseEntity.ok(GlobalResponse.success());
    }

}


