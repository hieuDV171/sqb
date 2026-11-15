package com.frozenheart.backend.modules.feedback.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.feedback.dto.FeedbackRequest;
import com.frozenheart.backend.modules.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @Operation(summary = "Gửi góp ý về web SQB")
    @PostMapping("/feedback")
    public GlobalResponse<Void> sendFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.send(JwtPayload.getCurrentUserPayload().getUserId(), request);
        return GlobalResponse.success();
    }
}
