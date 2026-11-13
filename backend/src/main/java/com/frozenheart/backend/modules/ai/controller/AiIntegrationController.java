package com.frozenheart.backend.modules.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiApplyResponse;
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

@Tag(name = "24. Tích hợp Trí tuệ Nhân tạo (AI Integration)", description = "API tinh chỉnh câu hỏi thi và phát hiện ảo giác (Btprop)")
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
