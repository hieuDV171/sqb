package com.frozenheart.backend.modules.session.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.frozenheart.backend.modules.session.dto.SessionQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.SubmissionsResponse;
import com.frozenheart.backend.modules.session.service.QuestionInteractionService;
import com.frozenheart.backend.modules.session.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class QuestionInteractionController {

    private final QuestionInteractionService questionInteractionService;
    private final SessionService sessionService;

    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<GlobalResponse<SubmissionsResponse>> getUserProposedSessions(
            @PathVariable Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, name = "subject_id") Long subjectId,
            @RequestParam(required = false) SessionStatus status) {

        SubmissionsResponse response = sessionService.getUserSubmissions(userId, after, limit, subjectId, status);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/sessions/{sessionId}/questions")
    public ResponseEntity<GlobalResponse<SessionQuestionsResponse>> getSessionQuestions(
            @PathVariable Long sessionId) {

        SessionQuestionsResponse response = questionInteractionService.getSessionQuestions(sessionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<GlobalResponse<AnswerQuestionResponse>> answerQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerQuestionRequest request) {

        AnswerQuestionResponse response = questionInteractionService.answerQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/questions/{questionId}/statistics")
    public ResponseEntity<GlobalResponse<QuestionStatisticsResponse>> getQuestionStatistics(
            @PathVariable Long questionId) {

        QuestionStatisticsResponse response = questionInteractionService.getQuestionStatistics(questionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/questions/{questionId}/rate")
    public ResponseEntity<GlobalResponse<RateQuestionResponse>> rateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody RateQuestionRequest request) {

        RateQuestionResponse response = questionInteractionService.rateQuestion(questionId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/questions/{questionId}/ratings")
    public ResponseEntity<GlobalResponse<QuestionRatingsResponse>> getQuestionRatings(
            @PathVariable Long questionId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        QuestionRatingsResponse response = questionInteractionService.getQuestionRatings(questionId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
