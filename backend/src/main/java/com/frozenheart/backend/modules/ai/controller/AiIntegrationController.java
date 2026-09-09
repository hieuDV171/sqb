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
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiApplyResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatHistoryResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatRequest;
import com.frozenheart.backend.modules.ai.dto.AiChatResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatSessionSummaryResponse;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;
import com.frozenheart.backend.modules.ai.service.AiIntegrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AiIntegrationController {

    private final AiIntegrationService aiIntegrationService;

    @PostMapping("/questions/{questionId}/ai-refine")
    public ResponseEntity<GlobalResponse<AiRefineResponse>> refineQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody AiRefineRequest request) {
        AiRefineResponse response = aiIntegrationService.refineQuestion(questionId, request, EditActorType.LECTURER);
        return ResponseEntity.ok(GlobalResponse.success("AI tinh chỉnh câu hỏi thành công", response));
    }

    @PostMapping("/admin/ai/chat")
    public ResponseEntity<GlobalResponse<AiChatResponse>> chatWithAi(
            @Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiIntegrationService.chatWithAi(request);
        return ResponseEntity.ok(GlobalResponse.success("Phản hồi từ AI thành công", response));
    }

    @GetMapping("/admin/ai/chat/history")
    public ResponseEntity<GlobalResponse<CursorResponse<AiChatHistoryResponse>>> getChatHistory(
            @RequestParam String sessionId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        CursorResponse<AiChatHistoryResponse> history = aiIntegrationService.getChatHistory(sessionId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success("Lấy lịch sử hội thoại AI thành công", history));
    }

    @GetMapping("/admin/ai/chat/sessions")
    public ResponseEntity<GlobalResponse<CursorResponse<AiChatSessionSummaryResponse>>> getUserChatSessions(
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "20") int limit) {
        CursorResponse<AiChatSessionSummaryResponse> sessions = aiIntegrationService.getUserChatSessions(after, limit);
        return ResponseEntity.ok(GlobalResponse.success("Lấy danh sách các cuộc hội thoại AI thành công", sessions));
    }

    @PostMapping("/questions/{questionId}/ai-apply")
    public ResponseEntity<GlobalResponse<AiApplyResponse>> applyAiRefinement(
            @PathVariable Long questionId,
            @Valid @RequestBody AiApplyRequest request) {
        AiApplyResponse response = aiIntegrationService.applyAiRefinement(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success("Áp dụng kết quả chỉnh sửa AI thành công", response));
    }
}
