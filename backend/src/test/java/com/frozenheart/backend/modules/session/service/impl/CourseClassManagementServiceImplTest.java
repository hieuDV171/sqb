package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserCourseClass;
import com.frozenheart.backend.core.entity.user.UserCourseClassId;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.session.dto.ExcelParsedClassData;
import com.frozenheart.backend.modules.session.dto.ExcelStudentRow;
import com.frozenheart.backend.modules.session.dto.AssignStudentsRequest;
import com.frozenheart.backend.modules.session.dto.CreateCourseClassRequest;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.service.ExcelParserService;
import com.frozenheart.backend.modules.user.repository.UserCourseClassRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseClassManagementServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private CourseClassRepository courseClassRepository;
    @Mock
    private UserCourseClassRepository userCourseClassRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserGamificationRepository userGamificationRepository;
    @Mock
    private UserPushSettingRepository userPushSettingRepository;
    @Mock
    private CurrentSemesterHolder currentSemesterHolder;
    @Mock
    private ExcelParserService excelParserService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseClassManagementServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Import lại lớp đã tồn tại phải áp dụng lecturer_id được Admin chọn")
    void importExistingClassShouldUpdateExplicitLecturer() {
        Long adminId = 1L;
        Long studentId = 7L;
        Long newLecturerId = 9L;
        setCurrentAdmin(adminId);

        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        ExcelStudentRow row = ExcelStudentRow.builder()
                .rowNumber(1)
                .studentCode("20250001")
                .fullName("Sinh viên kiểm thử")
                .email("student@sqb.edu.vn")
                .className("K68")
                .major("CNTT")
                .build();
        ExcelParsedClassData parsedData = ExcelParsedClassData.builder()
                .semesterName("20261")
                .classCode("IT4788-01")
                .subjectCode("IT4788")
                .lecturerName("Giảng viên cũ")
                .students(List.of(row))
                .build();

        User student = User.builder().id(studentId).email(row.email()).role(UserRole.STUDENT).build();
        UserProfile studentProfile = UserProfile.builder()
                .userId(studentId)
                .user(student)
                .studentLecturerCode(row.studentCode())
                .build();
        User oldLecturer = User.builder().id(8L).role(UserRole.LECTURER).build();
        User newLecturer = User.builder().id(newLecturerId).role(UserRole.LECTURER).build();
        Semester semester = Semester.builder().id(61L).name("20261").build();
        Subject subject = Subject.builder().id(11L).code("IT4788").name("Phát triển ứng dụng đa nền tảng").build();
        CourseClass existingClass = CourseClass.builder()
                .id(21L)
                .classCode(parsedData.classCode())
                .subject(subject)
                .semester(semester)
                .lecturer(oldLecturer)
                .createdAt(Instant.now())
                .build();
        UserCourseClass enrollment = UserCourseClass.builder()
                .id(new UserCourseClassId(studentId, existingClass.getId()))
                .user(student)
                .courseClass(existingClass)
                .build();

        when(excelParserService.parseCourseClassExcel(file)).thenReturn(parsedData);
        when(userRepository.findExistingEmailsByEmailIn(any())).thenReturn(Set.of(row.email()));
        when(userProfileRepository.findAllByStudentLecturerCodeIn(any())).thenReturn(List.of(studentProfile));
        when(currentSemesterHolder.getCurrentSemester()).thenReturn(semester);
        when(subjectRepository.findByCode("IT4788")).thenReturn(Optional.of(subject));
        when(courseClassRepository.findByClassCodeAndSemesterId(parsedData.classCode(), semester.getId()))
                .thenReturn(Optional.of(existingClass));
        when(userRepository.findById(newLecturerId)).thenReturn(Optional.of(newLecturer));
        when(courseClassRepository.save(existingClass)).thenReturn(existingClass);
        when(userCourseClassRepository.findByIdCourseClassIdAndIdUserIdIn(eq(existingClass.getId()), any()))
                .thenReturn(List.of(enrollment));

        service.importAndEnrollFromExcel(file, newLecturerId);

        ArgumentCaptor<CourseClass> captor = ArgumentCaptor.forClass(CourseClass.class);
        verify(courseClassRepository).save(captor.capture());
        assertEquals(newLecturerId, captor.getValue().getLecturer().getId());
    }

    @Test
    @DisplayName("Giảng viên không được tạo lớp cho giảng viên khác hoặc học kỳ tùy chọn")
    void lecturerCannotOverrideLecturerOrSemesterWhenCreatingClass() {
        setCurrentLecturer(20L);

        CreateCourseClassRequest otherLecturerRequest = new CreateCourseClassRequest(
                "IT4788-01", 11L, null, 99L);
        CreateCourseClassRequest customSemesterRequest = new CreateCourseClassRequest(
                "IT4788-01", 11L, 61L, null);

        assertThrows(AppException.class, () -> service.createCourseClass(otherLecturerRequest));
        assertThrows(AppException.class, () -> service.createCourseClass(customSemesterRequest));
        verify(courseClassRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giảng viên không được xem chi tiết lớp do người khác phụ trách")
    void lecturerCannotOpenAnotherLecturersClass() {
        setCurrentLecturer(20L);
        CourseClass courseClass = CourseClass.builder()
                .id(30L)
                .lecturer(User.builder().id(21L).role(UserRole.LECTURER).active(true).build())
                .build();
        when(courseClassRepository.findByIdFetchAll(30L)).thenReturn(Optional.of(courseClass));

        assertThrows(AppException.class, () -> service.getCourseClassDetail(30L));
        verify(userCourseClassRepository, never()).countByIdCourseClassId(any());
    }

    @Test
    @DisplayName("Gán sinh viên phải batch xác thực và bỏ qua tài khoản không phải STUDENT")
    void assignStudentsShouldOnlyEnrollActiveStudents() {
        Long lecturerId = 20L;
        Long courseClassId = 30L;
        Long studentId = 40L;
        Long invalidLecturerId = 41L;
        Long missingUserId = 42L;
        setCurrentLecturer(lecturerId);

        User owner = User.builder().id(lecturerId).role(UserRole.LECTURER).active(true).build();
        User student = User.builder().id(studentId).role(UserRole.STUDENT).active(true).build();
        User invalidLecturer = User.builder().id(invalidLecturerId).role(UserRole.LECTURER).active(true).build();
        CourseClass courseClass = CourseClass.builder()
                .id(courseClassId)
                .lecturer(owner)
                .build();

        when(courseClassRepository.findByIdFetchAll(courseClassId)).thenReturn(Optional.of(courseClass));
        when(userRepository.findAllById(any())).thenReturn(List.of(student, invalidLecturer));
        when(userCourseClassRepository.findByIdCourseClassIdAndIdUserIdIn(eq(courseClassId), any()))
                .thenReturn(List.of());
        when(userRepository.getReferenceById(studentId)).thenReturn(student);

        var response = service.assignStudentsToClass(courseClassId,
                new AssignStudentsRequest(null, List.of(studentId, invalidLecturerId, missingUserId)));

        assertEquals(1, response.totalEnrolled());
        assertEquals(2, response.notFoundCount());
        verify(userCourseClassRepository).saveAll(any());
    }

    private void setCurrentAdmin(Long userId) {
        JwtPayload payload = JwtPayload.builder().userId(userId).role(UserRole.ADMIN.name()).build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setCurrentLecturer(Long userId) {
        JwtPayload payload = JwtPayload.builder().userId(userId).role(UserRole.LECTURER.name()).build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("lecturer", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
