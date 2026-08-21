package com.frozenheart.backend.modules.session.controller;

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
import com.frozenheart.backend.modules.session.dto.MySubmissionsResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.dto.UpdateSubmissionSessionRequest;
import com.frozenheart.backend.modules.session.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/propose")
    public ResponseEntity<GlobalResponse<ProposeSessionResponse>> proposeSession(
            @Valid @RequestBody ProposeSessionRequest request) {

        ProposeSessionResponse response = sessionService.proposeSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<GlobalResponse<MySubmissionsResponse>> getMySubmissions(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, name = "subject_id") Long subjectId,
            @RequestParam(required = false) SessionStatus status) {

        MySubmissionsResponse response = sessionService.getMySubmissions(after, limit, subjectId, status);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/my-submissions/{sessionId}")
    public ResponseEntity<GlobalResponse<MySubmissionDetailResponse>> getMySubmissionDetail(
            @PathVariable Long sessionId) {

        MySubmissionDetailResponse response = sessionService.getMySubmissionDetail(sessionId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/my-submissions/{sessionId}")
    public ResponseEntity<GlobalResponse<Void>> updateMySubmissionSession(
            @PathVariable Long sessionId,
            @RequestBody UpdateSubmissionSessionRequest request) {

        sessionService.updateMySubmissionSession(sessionId, request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @DeleteMapping("/my-submissions/{sessionId}")
    public ResponseEntity<GlobalResponse<Void>> deleteMySubmissionSession(
            @PathVariable Long sessionId) {

        sessionService.deleteMySubmissionSession(sessionId);
        return ResponseEntity.ok(GlobalResponse.success());
    }

}


