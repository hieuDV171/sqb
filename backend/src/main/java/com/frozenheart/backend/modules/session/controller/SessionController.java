package com.frozenheart.backend.modules.session.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
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

}
