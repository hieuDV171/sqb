package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.CreateSemesterRequest;
import com.frozenheart.backend.modules.session.dto.SemesterResponse;
import com.frozenheart.backend.modules.session.service.SemesterManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/semesters")
@RequiredArgsConstructor
public class AdminSemesterController {

    private final SemesterManagementService semesterManagementService;

    @PostMapping
    public ResponseEntity<GlobalResponse<SemesterResponse>> createSemester(
            @Valid @RequestBody CreateSemesterRequest request) {
        SemesterResponse response = semesterManagementService.createSemester(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<List<SemesterResponse>>> getAllSemesters() {
        List<SemesterResponse> response = semesterManagementService.getAllSemesters();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/{semesterId}/activate")
    public ResponseEntity<GlobalResponse<SemesterResponse>> activateSemester(
            @PathVariable Long semesterId) {
        SemesterResponse response = semesterManagementService.activateSemester(semesterId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/deactive-all")
    public ResponseEntity<GlobalResponse<String>> deactivateAllSemesters() {
        semesterManagementService.deactivateAllSemesters();
        return ResponseEntity.ok(GlobalResponse.success("Đã hủy kích hoạt toàn bộ học kỳ"));
    }

    @DeleteMapping("/{semesterId}")
    public ResponseEntity<GlobalResponse<String>> deleteSemester(@PathVariable Long semesterId) {
        semesterManagementService.deleteSemester(semesterId);
        return ResponseEntity.ok(GlobalResponse.success("Đã xóa học kỳ thành công"));
    }
}
