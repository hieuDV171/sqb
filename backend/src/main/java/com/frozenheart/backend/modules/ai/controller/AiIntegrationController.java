package com.frozenheart.backend.modules.ai.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiApplyResponse;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;
import com.frozenheart.backend.modules.ai.service.AiIntegrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Tag(name = "24. Tích hợp Trí tuệ Nhân tạo (AI Integration)", description = "API tinh chỉnh câu hỏi thi và phát hiện ảo giác (Btprop)")
@RestController
@RequiredArgsConstructor
public class AiIntegrationController {

    private final AiIntegrationService aiIntegrationService;
    private final JsonMapper jsonMapper;

    @Operation(summary = "AI tinh chỉnh câu hỏi trắc nghiệm", description = "Sử dụng LLM để hoàn thiện nội dung câu hỏi, điều chỉnh phương án nhiễu, viết giải thích chi tiết và tự động chạy quy trình kiểm định ảo giác Btprop.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI tinh chỉnh câu hỏi thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền duyệt lớp hoặc lời nhắc vi phạm chính sách"),
            @ApiResponse(responseCode = "409", description = "Phiên đã chốt kết quả duyệt"),
            @ApiResponse(responseCode = "404", description = "Câu hỏi không tồn tại (QUESTION_NOT_FOUND)")
    })
    @PostMapping("/questions/{questionId}/ai-refine")
    public ResponseEntity<GlobalResponse<AiRefineResponse>> refineQuestion(
            @Parameter(description = "ID của câu hỏi cần tinh chỉnh", example = "42") @PathVariable Long questionId,
            @Valid @RequestBody AiRefineRequest request) {
        AiRefineResponse response = aiIntegrationService.refineQuestion(questionId, request, EditActorType.LECTURER);
        return ResponseEntity.ok(GlobalResponse.success("AI tinh chỉnh câu hỏi thành công", response));
    }

    @Operation(summary = "Nhận bản tinh chỉnh AI qua SSE",
            description = """
                    Mỗi sự kiện có một dòng `data:` chứa JSON với hai trường `type` và `data`.

                    - `proposal` gửi bản xem trước khi BTProp hoàn tất kiểm chứng.
                    - `complete` gửi kết quả kiểm chứng và bản đề xuất đã lưu.
                    - `error` gửi thông báo trong `data.message` nếu lỗi xảy ra sau khi luồng bắt đầu.

                    Lỗi trước khi bắt đầu luồng trả về phản hồi HTTP thông thường.
                    """)
    @ApiResponse(responseCode = "200", description = "Luồng sự kiện của lượt tinh chỉnh",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")))
    @PostMapping("/questions/{questionId}/ai-refine/stream")
    public void streamRefinement(
            @Parameter(description = "ID của câu hỏi cần tinh chỉnh", example = "42") @PathVariable Long questionId,
            @Valid @RequestBody AiRefineRequest request,
            HttpServletResponse response) {
        try {
            var result = aiIntegrationService.refineQuestion(questionId, request, EditActorType.LECTURER,
                    preview -> sendRefinementEvent(response, "proposal", preview));
            sendRefinementEvent(response, "complete", result);
        } catch (UncheckedIOException e) {
            log.debug("AI refinement stream closed for question {}", questionId, e);
        } catch (RuntimeException e) {
            if (!response.isCommitted()) throw e;
            String message = e instanceof AppException appException ? appException.getMessage()
                    : "Không hoàn tất được lượt tinh chỉnh. Hãy gửi lại yêu cầu.";
            log.warn("AI refinement did not complete for question {}", questionId, e);
            try {
                sendRefinementEvent(response, "error", Map.of("message", message));
            } catch (UncheckedIOException disconnected) {
                log.debug("AI refinement stream closed for question {}", questionId, disconnected);
            }
        }
    }

    private void sendRefinementEvent(HttpServletResponse response, String type, Object data) {
        String event = "data: " + jsonMapper.writeValueAsString(Map.of("type", type, "data", data)) + "\n\n";
        if (!response.isCommitted()) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-transform");
        }
        try {
            response.getOutputStream().write(event.getBytes(StandardCharsets.UTF_8));
            response.flushBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Operation(summary = "Phê duyệt hoặc loại bỏ kết quả chỉnh sửa của AI", description = "Giảng viên/Người kiểm duyệt chấp thuận để ghi đè nội dung do AI đề xuất vào câu hỏi gốc hoặc từ chối.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Áp dụng kết quả chỉnh sửa AI thành công"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy nhật ký chỉnh sửa AI (INVALID_PARAMETER_VALUE)"),
            @ApiResponse(responseCode = "403", description = "Không có quyền duyệt lớp"),
            @ApiResponse(responseCode = "404", description = "Câu hỏi liên kết không tồn tại (QUESTION_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Phiên đã chốt hoặc bản AI đã được xử lý"),
            @ApiResponse(responseCode = "412", description = "Câu hỏi đã thay đổi sau khi yêu cầu tinh chỉnh"),
            @ApiResponse(responseCode = "422", description = "Cần xác nhận đã tự rà soát cảnh báo BTProp")
    })
    @PostMapping("/questions/{questionId}/ai-apply")
    public ResponseEntity<GlobalResponse<AiApplyResponse>> applyAiRefinement(
            @Parameter(description = "ID của câu hỏi", example = "42") @PathVariable Long questionId,
            @Valid @RequestBody AiApplyRequest request) {
        AiApplyResponse response = aiIntegrationService.applyAiRefinement(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success("Áp dụng kết quả chỉnh sửa AI thành công", response));
    }
}
