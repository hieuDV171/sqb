package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.CreateSubjectRequest;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/subjects")
@RequiredArgsConstructor
public class AdminSubjectController {

    private final CourseClassManagementService courseClassManagementService;

    @PostMapping
    public ResponseEntity<GlobalResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse response = courseClassManagementService.createSubject(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<List<SubjectResponse>>> getAllSubjects() {
        List<SubjectResponse> response = courseClassManagementService.getAllSubjects();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
