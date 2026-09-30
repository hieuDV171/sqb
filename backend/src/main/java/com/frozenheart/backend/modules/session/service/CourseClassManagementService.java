package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseClassManagementService {

    // 1. Quản lý môn học (Subject)
    SubjectResponse createSubject(CreateSubjectRequest request);

    List<SubjectResponse> getAllSubjects();

    // 2. Quản lý lớp học phần (CourseClass)
    CourseClassResponse createCourseClass(CreateCourseClassRequest request);

    List<CourseClassResponse> getCourseClassesByActiveSemester();

    List<CourseClassResponse> getMyCourseClasses();

    CourseClassResponse getCourseClassDetail(Long courseClassId);

    // 3. Gán sinh viên & danh sách sinh viên lớp học phần
    AssignStudentsResponse assignStudentsToClass(Long courseClassId, AssignStudentsRequest request);

    List<ClassStudentResponse> getClassStudents(Long courseClassId);

    // 4. Import 2 giai đoạn từ file Excel
    ExcelImportClassResult importAndEnrollFromExcel(MultipartFile file, Long lecturerId);

}
