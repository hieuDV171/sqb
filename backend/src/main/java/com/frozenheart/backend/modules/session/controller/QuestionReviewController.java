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
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.EditQuestionRequest;
import com.frozenheart.backend.modules.session.dto.EditQuestionResponse;
import com.frozenheart.backend.modules.session.dto.PendingSessionsResponse;
import com.frozenheart.backend.modules.session.dto.RejectQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.SessionDetailReviewResponse;
import com.frozenheart.backend.modules.session.service.QuestionReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class QuestionReviewController {

    private final QuestionReviewService questionReviewService;

    @GetMapping("/sessions/pending")
    public ResponseEntity<GlobalResponse<PendingSessionsResponse>> getPendingSessions(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, name = "subject_id") Long subjectId,
            @RequestParam(required = false, name = "sort_by") String sortBy) {

        PendingSessionsResponse response = questionReviewService.getPendingSessions(after, limit, subjectId, sortBy);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<GlobalResponse<SessionDetailReviewResponse>> getSessionDetailForReview(
            @PathVariable Long sessionId) {

        SessionDetailReviewResponse response = questionReviewService.getSessionDetailForReview(sessionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/sessions/approve")
    public ResponseEntity<GlobalResponse<ApproveQuestionsResponse>> approveQuestions(
            @Valid @RequestBody ApproveQuestionsRequest request) {

        ApproveQuestionsResponse response = questionReviewService.approveQuestions(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/sessions/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectQuestions(
            @Valid @RequestBody RejectQuestionsRequest request) {

        questionReviewService.rejectQuestions(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<GlobalResponse<EditQuestionResponse>> editQuestion(
            @PathVariable Long questionId,
            @RequestBody EditQuestionRequest request) {

        EditQuestionResponse response = questionReviewService.editQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/sessions/{sessionId}/complete-review")
    public ResponseEntity<GlobalResponse<Void>> completeSessionReview(
            @PathVariable Long sessionId) {
        questionReviewService.completeSessionReview(sessionId);
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
