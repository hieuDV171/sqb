package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionRejectionCode;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.UserCourseClass;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.session.dto.MySubmissionDetailResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.dto.SubmissionProjection;
import com.frozenheart.backend.modules.session.dto.SubmissionsResponse;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;
import com.frozenheart.backend.modules.user.repository.UserCourseClassRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private AnonymizerUtil anonymizerUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseClassRepository courseClassRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionMediaRepository questionMediaRepository;
    @Mock
    private DuplicateDetectionService duplicateDetectionService;
    @Mock
    private CounterMetricsService counterMetricsService;
    @Mock
    private MetricService metricService;
    @Mock
    private UserCourseClassRepository userCourseClassRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private CurrentSemesterHolder currentSemesterHolder;

    @InjectMocks
    private SessionServiceImpl sessionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Phải gán displayOrder cho mọi câu hỏi trước khi lưu")
    void proposeSessionShouldAssignDisplayOrderBeforeSavingQuestions() {
        Long userId = 7L;
        Long subjectId = 11L;
        Long courseClassId = 21L;
        setCurrentUser(userId);

        User proposer = User.builder().id(userId).role(UserRole.ADMIN).build();
        Subject subject = Subject.builder().id(subjectId).code("IT4788").name("Phát triển ứng dụng đa nền tảng").build();
        Semester activeSemester = Semester.builder().id(61L).name("20261").active(true).build();
        CourseClass courseClass = courseClass(courseClassId, activeSemester, subject);

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(proposer));
        when(courseClassRepository.findByIdFetchAll(courseClassId)).thenReturn(Optional.of(courseClass));
        when(currentSemesterHolder.getCurrentSemester()).thenReturn(activeSemester);
        when(anonymizerUtil.encodeUserId(userId)).thenReturn("ABC123");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(101L);
            return session;
        });
        when(questionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Question> questions = invocation.getArgument(0);
            assertEquals(List.of(1, 2), questions.stream().map(Question::getDisplayOrder).toList());
            for (int index = 0; index < questions.size(); index++) {
                questions.get(index).setId(201L + index);
            }
            return questions;
        });

        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên câu hỏi kiểm thử",
                "Kiểm tra thứ tự câu hỏi",
                null,
                courseClassId,
                List.of(question("Câu hỏi thứ nhất"), question("Câu hỏi thứ hai")));

        ProposeSessionResponse response = sessionService.proposeSession(request);

        assertNotNull(response);
        assertEquals(101L, response.sessionId());
        assertEquals(courseClassId, response.courseClassId());
        assertEquals(2, response.questionCount());
        verify(questionRepository).saveAll(anyList());
        verify(counterMetricsService).incrementProposedQuestions(userId, 2);
        verify(duplicateDetectionService).asyncCheckDuplicates(101L);
    }

    @Test
    @DisplayName("Phải từ chối request null trước khi truy cập repository")
    void proposeSessionShouldRejectNullRequestBeforeRepositoryAccess() {
        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(null));

        assertEquals(ResponseCode.MISSING_REQUIRED_PARAMETER, exception.getResponseCode());
        assertEquals("Dữ liệu đề xuất phiên không được để trống", exception.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("Phải từ chối danh sách câu hỏi null trước khi truy cập repository")
    void proposeSessionShouldRejectNullQuestionsBeforeRepositoryAccess() {
        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên thiếu danh sách câu hỏi",
                null,
                null,
                11L,
                null);

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(request));

        assertEquals(ResponseCode.MISSING_REQUIRED_PARAMETER, exception.getResponseCode());
        assertEquals("Danh sách câu hỏi không được để trống", exception.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("Phải từ chối danh sách câu hỏi rỗng trước khi truy cập repository")
    void proposeSessionShouldRejectEmptyQuestionsBeforeRepositoryAccess() {
        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên không có câu hỏi",
                null,
                null,
                11L,
                List.of());

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(request));

        assertEquals(ResponseCode.MISSING_REQUIRED_PARAMETER, exception.getResponseCode());
        assertEquals("Danh sách câu hỏi không được để trống", exception.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("Phải từ chối đề xuất cho môn chỉ tham gia ở học kỳ cũ")
    void proposeSessionShouldRejectSubjectFromInactiveSemester() {
        Long userId = 7L;
        Long subjectId = 11L;
        Long courseClassId = 21L;
        setCurrentUser(userId);

        User proposer = User.builder().id(userId).role(UserRole.STUDENT).build();
        Subject subject = Subject.builder().id(subjectId).code("IT4788").name("Môn học kỳ cũ").build();
        Semester oldSemester = Semester.builder().id(52L).name("20252").build();
        Semester activeSemester = Semester.builder().id(61L).name("20261").active(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(proposer));
        CourseClass oldCourseClass = courseClass(courseClassId, oldSemester, subject);
        when(courseClassRepository.findByIdFetchAll(courseClassId)).thenReturn(Optional.of(oldCourseClass));
        when(currentSemesterHolder.getCurrentSemester()).thenReturn(activeSemester);

        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên sai học kỳ",
                null,
                null,
                courseClassId,
                List.of(question("Câu hỏi kiểm thử")));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(request));

        assertEquals(ResponseCode.INVALID_PARAMETER_VALUE, exception.getResponseCode());
        assertEquals(
                "Chỉ có thể đề xuất câu hỏi cho lớp học phần thuộc học kỳ hiện tại",
                exception.getMessage());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Danh sách lớp đề xuất gồm cả lịch sử nhưng chỉ lớp học kỳ hiện tại được chọn")
    void getMyEnrolledCourseClassesShouldMarkOnlyActiveSemesterClassAsSelectable() {
        Long userId = 7L;
        setCurrentUser(userId);

        Semester oldSemester = Semester.builder().id(52L).name("20252").build();
        Semester activeSemester = Semester.builder().id(61L).name("20261").active(true).build();
        Subject oldSubject = Subject.builder().id(11L).code("IT4788").name("Môn học kỳ cũ").build();
        Subject activeSubject = Subject.builder().id(12L).code("IT4999").name("Môn học kỳ hiện tại").build();

        when(currentSemesterHolder.getCurrentSemester()).thenReturn(activeSemester);
        when(userCourseClassRepository.findByUserIdFetchCourseClassAndSubject(userId))
                .thenReturn(List.of(
                        enrollment(21L, oldSemester, oldSubject),
                        enrollment(22L, activeSemester, activeSubject)));

        var courseClasses = sessionService.getMyEnrolledCourseClasses();

        assertEquals(2, courseClasses.size());
        assertEquals(activeSubject.getId(), courseClasses.getFirst().subjectId());
        assertTrue(courseClasses.getFirst().canPropose());
        assertEquals(oldSubject.getId(), courseClasses.getLast().subjectId());
        assertFalse(courseClasses.getLast().canPropose());
    }

    @Test
    @DisplayName("Phải trả trạng thái thực tế trong danh sách phiên của người dùng")
    void getMySubmissionsShouldReturnActualSessionStatus() {
        Long userId = 7L;
        setCurrentUser(userId);

        SubmissionProjection projection = mock(SubmissionProjection.class);
        when(projection.getSessionId()).thenReturn(101L);
        when(projection.getSessionCode()).thenReturn("IT4788_ABC123_1_101");
        when(projection.getTitle()).thenReturn("Phiên đang được xem xét");
        when(projection.getContent()).thenReturn("Nội dung kiểm thử");
        when(projection.getSubjectId()).thenReturn(11L);
        when(projection.getSubjectName()).thenReturn("Phát triển ứng dụng đa nền tảng");
        when(projection.getSubjectCode()).thenReturn("IT4788");
        when(projection.getStatus()).thenReturn(SessionStatus.REVIEWING);
        when(projection.getQuestionCount()).thenReturn(2);
        when(projection.getCreatedAt()).thenReturn(Instant.parse("2026-10-26T09:00:00Z"));
        when(projection.getReactCount()).thenReturn(0);
        when(projection.getCommentCount()).thenReturn(0);
        when(sessionRepository.findSubmissions(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(projection));

        SubmissionsResponse response = sessionService.getMySubmissions(null, 10, null, null);

        assertEquals(1, response.contents().size());
        assertEquals(SessionStatus.REVIEWING, response.contents().getFirst().status());
    }

    @Test
    @DisplayName("Phải trả chi tiết phiên chưa có giảng viên duyệt mà không truy vấn hồ sơ reviewer")
    void getMySubmissionDetailShouldHandleSessionWithoutReviewer() {
        Long userId = 7L;
        Long sessionId = 101L;
        setCurrentUser(userId);

        Session session = detailSession(userId, sessionId, null);
        when(sessionRepository.findByIdFetchCourseClassProposerAndReviewer(sessionId))
                .thenReturn(Optional.of(session));
        Question rejectedQuestion = Question.builder()
                .id(501L)
                .content("Câu hỏi bị trùng")
                .status(QuestionStatus.REJECTED)
                .rejectionCode(QuestionRejectionCode.DUPLICATE)
                .rejectionReason("Đã tồn tại trong ngân hàng")
                .displayOrder(1)
                .build();
        when(questionRepository.findQuestionsBySessionId(sessionId)).thenReturn(List.of(rejectedQuestion));

        MySubmissionDetailResponse response = sessionService.getMySubmissionDetail(sessionId);

        assertEquals(sessionId, response.sessionId());
        assertEquals("IT4788", response.subjectCode());
        assertEquals(SessionStatus.PENDING, response.status());
        assertNull(response.reviewedByLecturer());
        assertEquals(1, response.questions().size());
        assertEquals(QuestionStatus.REJECTED, response.questions().getFirst().status());
        assertEquals(QuestionRejectionCode.DUPLICATE, response.questions().getFirst().rejectionCode());
        assertEquals("Đã tồn tại trong ngân hàng", response.questions().getFirst().rejectionReason());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("Phải lấy fullName của giảng viên duyệt từ UserProfile")
    void getMySubmissionDetailShouldUseReviewerProfileFullName() {
        Long userId = 7L;
        Long reviewerId = 8L;
        Long sessionId = 101L;
        setCurrentUser(userId);

        User reviewer = User.builder()
                .id(reviewerId)
                .email("lecturer@sqb.edu.vn")
                .role(UserRole.LECTURER)
                .build();
        Session session = detailSession(userId, sessionId, reviewer);
        UserProfile reviewerProfile = UserProfile.builder()
                .userId(reviewerId)
                .fullName("TS. Nguyễn Văn B")
                .build();

        when(sessionRepository.findByIdFetchCourseClassProposerAndReviewer(sessionId))
                .thenReturn(Optional.of(session));
        when(questionRepository.findQuestionsBySessionId(sessionId)).thenReturn(List.of());
        when(userProfileRepository.findById(reviewerId)).thenReturn(Optional.of(reviewerProfile));

        MySubmissionDetailResponse response = sessionService.getMySubmissionDetail(sessionId);

        assertEquals("TS. Nguyễn Văn B", response.reviewedByLecturer());
        verify(userProfileRepository).findById(reviewerId);
    }

    private ProposeSessionRequest.QuestionProposeDto question(String content) {
        QuestionOption option = new QuestionOption();
        option.setKey("A");
        option.setText("Đáp án đúng");
        option.setIsCorrect(true);

        return new ProposeSessionRequest.QuestionProposeDto(
                content,
                null,
                List.of(option),
                null,
                false,
                3.0);
    }

    private void setCurrentUser(Long userId) {
        JwtPayload payload = JwtPayload.builder().userId(userId).role(UserRole.ADMIN.name()).build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Session detailSession(Long proposerId, Long sessionId, User reviewer) {
        User proposer = User.builder()
                .id(proposerId)
                .email("student@sqb.edu.vn")
                .role(UserRole.STUDENT)
                .build();
        Subject subject = Subject.builder()
                .id(11L)
                .code("IT4788")
                .name("Phát triển ứng dụng đa nền tảng")
                .build();
        Semester semester = Semester.builder().id(61L).name("20261").active(true).build();
        CourseClass courseClass = courseClass(21L, semester, subject);

        return Session.builder()
                .id(sessionId)
                .sessionCode("IT4788_ABC123_1_101")
                .title("Phiên câu hỏi kiểm thử")
                .content("Nội dung kiểm thử")
                .proposer(proposer)
                .reviewer(reviewer)
                .courseClass(courseClass)
                .status(SessionStatus.PENDING)
                .createdAt(Instant.parse("2026-10-26T09:00:00Z"))
                .build();
    }

    private UserCourseClass enrollment(
            Long courseClassId,
            Semester semester,
            Subject subject) {
        CourseClass courseClass = courseClass(courseClassId, semester, subject);
        return UserCourseClass.builder()
                .courseClass(courseClass)
                .build();
    }

    private CourseClass courseClass(Long courseClassId, Semester semester, Subject subject) {
        return CourseClass.builder()
                .id(courseClassId)
                .classCode("CLASS-" + courseClassId)
                .semester(semester)
                .subject(subject)
                .build();
    }

    private void verifyNoRepositoryInteractions() {
        verifyNoInteractions(
                userRepository,
                courseClassRepository,
                sessionRepository,
                questionRepository,
                questionMediaRepository,
                userCourseClassRepository,
                userProfileRepository);
    }
}
