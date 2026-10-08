package com.frozenheart.backend.modules.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiApplyResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatHistoryResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatRequest;
import com.frozenheart.backend.modules.ai.dto.AiChatResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatSessionSummaryResponse;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;
import com.frozenheart.backend.modules.ai.service.AiIntegrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "24. Tích hợp Trí tuệ Nhân tạo (AI Integration)", description = "APIs trợ lý AI, tinh chỉnh câu hỏi thi và phát hiện ảo giác (Btprop)")
@RestController
@RequiredArgsConstructor
public class AiIntegrationController {

    private final AiIntegrationService aiIntegrationService;

    @Operation(summary = "AI tinh chỉnh câu hỏi trắc nghiệm", description = "Sử dụng LLM để hoàn thiện nội dung câu hỏi, điều chỉnh phương án nhiễu, viết giải thích chi tiết và tự động chạy quy trình kiểm định ảo giác Btprop.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI tinh chỉnh câu hỏi thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Lời nhắc vi phạm chính sách hoặc chứa dấu hiệu Prompt Injection (CONTENT_VIOLATES_POLICY)"),
            @ApiResponse(responseCode = "404", description = "Câu hỏi không tồn tại (QUESTION_NOT_FOUND)")
    })
    @PostMapping("/questions/{questionId}/ai-refine")
    public ResponseEntity<GlobalResponse<AiRefineResponse>> refineQuestion(
            @Parameter(description = "ID của câu hỏi cần tinh chỉnh", example = "42") @PathVariable Long questionId,
            @Valid @RequestBody AiRefineRequest request) {
        AiRefineResponse response = aiIntegrationService.refineQuestion(questionId, request, EditActorType.LECTURER);
        return ResponseEntity.ok(GlobalResponse.success("AI tinh chỉnh câu hỏi thành công", response));
    }

    @Operation(summary = "Trò chuyện tương tác với trợ lý AI", description = "Gửi lời nhắc và ngữ cảnh môn học đến mô hình AI nội bộ để nhận phản hồi học thuật.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phản hồi từ AI thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Lời nhắc vi phạm chính sách an toàn")
    })
    @PostMapping("/admin/ai/chat")
    public ResponseEntity<GlobalResponse<AiChatResponse>> chatWithAi(
            @Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiIntegrationService.chatWithAi(request);
        return ResponseEntity.ok(GlobalResponse.success("Phản hồi từ AI thành công", response));
    }

    @Operation(summary = "Lấy lịch sử hội thoại AI theo phiên", description = "Truy vấn danh sách tin nhắn giữa người dùng và AI trong một phiên chat cụ thể theo con trỏ phân trang.")
    @ApiResponse(responseCode = "200", description = "Lấy lịch sử hội thoại AI thành công")
    @GetMapping("/admin/ai/chat/history")
    public ResponseEntity<GlobalResponse<CursorResponse<AiChatHistoryResponse>>> getChatHistory(
            @Parameter(description = "Mã định danh phiên chat", example = "session_a1b2c3d4")
            @RequestParam(name = "session_id", required = false)
            String sessionId,
            @Parameter(description = "Con trỏ phân trang (ID tin nhắn)")
            @RequestParam(required = false)
            Long after,
            @Parameter(description = "Số lượng tin nhắn trả về mỗi trang", example = "20")
            @RequestParam(required = false, defaultValue = "20")
            int limit
    ) {
        CursorResponse<AiChatHistoryResponse> history = aiIntegrationService.getChatHistory(sessionId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success("Lấy lịch sử hội thoại AI thành công", history));
    }

    @Operation(summary = "Lấy danh sách các phiên trò chuyện AI của tôi", description = "Truy vấn danh sách các cuộc hội thoại AI trước đây của người dùng hiện tại.")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách các cuộc hội thoại AI thành công")
    @GetMapping("/admin/ai/chat/sessions")
    public ResponseEntity<GlobalResponse<CursorResponse<AiChatSessionSummaryResponse>>> getUserChatSessions(
            @Parameter(description = "Con trỏ phân trang (ID phiên chat)") @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng phiên chat trả về", example = "20") @RequestParam(defaultValue = "20") int limit) {
        CursorResponse<AiChatSessionSummaryResponse> sessions = aiIntegrationService.getUserChatSessions(after, limit);
        return ResponseEntity.ok(GlobalResponse.success("Lấy danh sách các cuộc hội thoại AI thành công", sessions));
    }

    @Operation(summary = "Phê duyệt hoặc loại bỏ kết quả chỉnh sửa của AI", description = "Giảng viên/Người kiểm duyệt chấp thuận để ghi đè nội dung do AI đề xuất vào câu hỏi gốc hoặc từ chối.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Áp dụng kết quả chỉnh sửa AI thành công"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy nhật ký chỉnh sửa AI (INVALID_PARAMETER_VALUE)"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện thao tác này (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Câu hỏi liên kết không tồn tại (QUESTION_NOT_FOUND)")
    })
    @PostMapping("/questions/{questionId}/ai-apply")
    public ResponseEntity<GlobalResponse<AiApplyResponse>> applyAiRefinement(
            @Parameter(description = "ID của câu hỏi", example = "42") @PathVariable Long questionId,
            @Valid @RequestBody AiApplyRequest request) {
        AiApplyResponse response = aiIntegrationService.applyAiRefinement(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success("Áp dụng kết quả chỉnh sửa AI thành công", response));
    }
}
