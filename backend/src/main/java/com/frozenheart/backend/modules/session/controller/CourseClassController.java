package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.dto.*;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/lecturer/course-classes")
@RequiredArgsConstructor
public class CourseClassController {

    private final CourseClassManagementService courseClassManagementService;

    @PostMapping
    public ResponseEntity<GlobalResponse<CourseClassResponse>> createCourseClass(
            @Valid @RequestBody CreateCourseClassRequest request) {
        CourseClassResponse response = courseClassManagementService.createCourseClass(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<List<CourseClassResponse>>> getCourseClasses() {
        List<CourseClassResponse> response = courseClassManagementService.getCourseClassesByActiveSemester();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/my-classes")
    public ResponseEntity<GlobalResponse<List<CourseClassResponse>>> getMyCourseClasses() {
        List<CourseClassResponse> response = courseClassManagementService.getMyCourseClasses();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/{courseClassId}")
    public ResponseEntity<GlobalResponse<CourseClassResponse>> getCourseClassDetail(
            @PathVariable Long courseClassId) {
        CourseClassResponse response = courseClassManagementService.getCourseClassDetail(courseClassId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/{courseClassId}/students")
    public ResponseEntity<GlobalResponse<AssignStudentsResponse>> assignStudents(
            @PathVariable Long courseClassId,
            @RequestBody AssignStudentsRequest request) {
        AssignStudentsResponse response = courseClassManagementService.assignStudentsToClass(courseClassId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/{courseClassId}/students")
    public ResponseEntity<GlobalResponse<List<ClassStudentResponse>>> getClassStudents(
            @PathVariable Long courseClassId) {
        List<ClassStudentResponse> response = courseClassManagementService.getClassStudents(courseClassId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<ExcelImportClassResult>> importCourseClassFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lecturerId", required = false) Long lecturerId) {
        ExcelImportClassResult response = courseClassManagementService.importAndEnrollFromExcel(file, lecturerId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/lecturers")
    public ResponseEntity<GlobalResponse<List<LecturerSummaryResponse>>> getLecturers() {
        List<LecturerSummaryResponse> response = courseClassManagementService.getAllActiveLecturers();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }


}

