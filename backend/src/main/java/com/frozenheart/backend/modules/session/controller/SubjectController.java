package com.frozenheart.backend.modules.session.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.service.SessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<SubjectResponse>>> getSubjectDropdownList() {
        List<SubjectResponse> response = sessionService.getSubjectList();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
