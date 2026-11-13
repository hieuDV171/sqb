package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionRejectionCode;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.EditQuestionRequest;
import com.frozenheart.backend.modules.session.dto.RejectQuestionsRequest;
import com.frozenheart.backend.modules.session.repository.QuestionEditLogRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionReviewServiceImplTest {

    @Mock SessionRepository sessionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionEditLogRepository questionEditLogRepository;
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock CounterMetricsService counterMetricsService;
    @Mock ActivityFeedService activityFeedService;
    @Mock AnonymizerUtil anonymizerUtil;
    @Mock GamificationService gamificationService;
    @Mock PostRepository postRepository;
    @Mock NotificationWriter notificationWriter;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock BadgeService badgeService;
    @Mock MetricService metricService;

    @InjectMocks QuestionReviewServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reviewableQueueIsScopedToAssignedAndSelectedCourseClass() {
        setCurrentUser(7L, UserRole.LECTURER);
        User lecturer = User.builder().id(7L).build();
        Subject subject = Subject.builder().id(11L).code("IT4788").name("Phát triển ứng dụng đa nền tảng").build();
        Semester semester = Semester.builder().id(61L).name("20261").active(true).build();
        CourseClass courseClass = CourseClass.builder()
                .id(20L)
                .classCode("IT4788-01")
                .lecturer(lecturer)
                .subject(subject)
                .semester(semester)
                .build();
        Session session = Session.builder()
                .id(30L)
                .courseClass(courseClass)
                .status(SessionStatus.REVIEWING)
                .title("Phiên cần duyệt")
                .build();
        when(sessionRepository.findReviewableSessionsFetchCourseClassAndProposerWithCursor(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.isNull(),
                any(Pageable.class)))
                .thenReturn(List.of(session));

        var response = service.getReviewableSessions(null, 10, 20L, "createdAt,desc");

        assertEquals(1, response.items().size());
        assertEquals("IT4788-01", response.items().getFirst().classCode());
        assertEquals("20261", response.items().getFirst().semesterName());
        verify(sessionRepository).findReviewableSessionsFetchCourseClassAndProposerWithCursor(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.isNull(),
                any(Pageable.class));
    }

    @Test
    void assignedLecturerCanRejectWithStructuredReason() {
        setCurrentUser(7L, UserRole.LECTURER);
        Question question = questionAssignedTo(101L, 7L);
        question.setStatus(QuestionStatus.APPROVED);
        when(questionRepository.findByIdInFetchSessionCourseClassAndProposer(List.of(101L)))
                .thenReturn(List.of(question));

        service.rejectQuestions(new RejectQuestionsRequest(
                List.of(101L), QuestionRejectionCode.DUPLICATE, "  Trùng câu đã có  "));

        assertEquals(QuestionStatus.REJECTED, question.getStatus());
        assertEquals(QuestionRejectionCode.DUPLICATE, question.getRejectionCode());
        assertEquals("Trùng câu đã có", question.getRejectionReason());
        assertEquals(SessionStatus.REVIEWING, question.getSession().getStatus());
        verify(questionRepository).saveAll(List.of(question));
        verifyNoInteractions(counterMetricsService, gamificationService, eventPublisher);
    }

    @Test
    void studentCannotUseReviewEndpoint() {
        setCurrentUser(9L, UserRole.STUDENT);

        AppException exception = assertThrows(AppException.class,
                () -> service.rejectQuestions(new RejectQuestionsRequest(
                        List.of(101L), QuestionRejectionCode.UNSPECIFIED, null)));

        assertEquals(ResponseCode.REVIEW_PERMISSION_DENIED, exception.getResponseCode());
        verifyNoInteractions(questionRepository);
    }

    @Test
    void lecturerCannotReviewAnotherLecturersClass() {
        setCurrentUser(7L, UserRole.LECTURER);
        Question question = questionAssignedTo(101L, 8L);
        when(questionRepository.findByIdInFetchSessionCourseClassAndProposer(List.of(101L)))
                .thenReturn(List.of(question));

        AppException exception = assertThrows(AppException.class,
                () -> service.rejectQuestions(new RejectQuestionsRequest(
                        List.of(101L), QuestionRejectionCode.LOW_QUALITY, null)));

        assertEquals(ResponseCode.REVIEW_PERMISSION_DENIED, exception.getResponseCode());
        verify(userRepository, never()).getReferenceById(7L);
        verify(questionRepository, never()).saveAll(anyList());
    }

    @Test
    void approvingQuestionClearsPreviousRejectionMetadata() {
        setCurrentUser(7L, UserRole.LECTURER);
        Question question = questionAssignedTo(101L, 7L);
        question.setStatus(QuestionStatus.REJECTED);
        question.setRejectionCode(QuestionRejectionCode.OTHER);
        question.setRejectionReason("Lý do cũ");
        when(questionRepository.findByIdInFetchSessionCourseClassAndProposer(List.of(101L)))
                .thenReturn(List.of(question));

        var response = service.approveQuestions(new ApproveQuestionsRequest(List.of(101L)));

        assertEquals(QuestionStatus.APPROVED, question.getStatus());
        assertNull(question.getRejectionCode());
        assertNull(question.getRejectionReason());
        assertEquals(SessionStatus.REVIEWING, question.getSession().getStatus());
        assertEquals(0.0, response.pointsEarned());
        verifyNoInteractions(counterMetricsService, gamificationService, eventPublisher);
    }

    @Test
    void chotWithNoPendingQuestionsFinalizesOnceAndRewardsOnlyOnFirstCall() {
        setCurrentUser(7L, UserRole.LECTURER);
        Question question = questionAssignedTo(101L, 7L);
        question.setStatus(QuestionStatus.APPROVED);
        Session session = question.getSession();
        session.setProposer(User.builder().id(9L).build());
        session.getCourseClass().setSubject(Subject.builder().id(10L).code("IT").name("Môn học").build());
        Semester semester = Semester.builder().id(22L).build();
        session.getCourseClass().setSemester(semester);
        when(sessionRepository.findByIdFetchCourseClassProposerAndReviewer(30L)).thenReturn(Optional.of(session));
        when(questionRepository.findBySessionId(30L)).thenReturn(List.of(question));
        when(userRepository.getReferenceById(7L)).thenReturn(User.builder().id(7L).build());
        when(postRepository.save(any(Post.class))).thenReturn(Post.builder().id(20L).build());

        service.completeSessionReview(30L);
        service.completeSessionReview(30L);

        assertEquals(SessionStatus.RESOLVED, session.getStatus());
        verify(sessionRepository, times(2)).lockReviewSessions(List.of(30L), false);
        verify(sessionRepository).save(session);
        verify(gamificationService).onSessionsFinalized(Map.of(session, List.of(question)));
        verify(counterMetricsService).awardPointsAndApprovedQuestionsBatch(anyList(), org.mockito.ArgumentMatchers.same(semester));
        verify(notificationWriter).create(any());
        verify(questionRepository, never()).saveAll(anyList());
    }

    @Test
    void chotRejectsPendingQuestionsWithoutRewardingAnAllRejectedSession() {
        setCurrentUser(7L, UserRole.LECTURER);
        Question question = questionAssignedTo(101L, 7L);
        Session session = question.getSession();
        when(sessionRepository.findByIdFetchCourseClassProposerAndReviewer(30L)).thenReturn(Optional.of(session));
        when(questionRepository.findBySessionId(30L)).thenReturn(List.of(question));
        when(userRepository.getReferenceById(7L)).thenReturn(User.builder().id(7L).build());

        service.completeSessionReview(30L);

        assertEquals(SessionStatus.RESOLVED, session.getStatus());
        assertEquals(QuestionStatus.REJECTED, question.getStatus());
        assertEquals(QuestionRejectionCode.AUTO_REJECTED_ON_COMPLETION, question.getRejectionCode());
        verify(questionRepository).saveAll(List.of(question));
        verifyNoInteractions(counterMetricsService, notificationWriter, postRepository);
    }

    @Test
    void finalizedSessionBlocksAllDecisionEndpointsEvenForAdmin() {
        setCurrentUser(7L, UserRole.ADMIN);
        Question question = questionAssignedTo(101L, 7L);
        question.getSession().setStatus(SessionStatus.RESOLVED);
        when(questionRepository.findSessionIdsForReview(List.of(101L))).thenReturn(List.of(30L));
        when(questionRepository.findByIdInFetchSessionCourseClassAndProposer(List.of(101L)))
                .thenReturn(List.of(question));

        assertEquals(ResponseCode.SESSION_REVIEW_FINALIZED, assertThrows(AppException.class,
                () -> service.approveQuestions(new ApproveQuestionsRequest(List.of(101L)))).getResponseCode());
        assertEquals(ResponseCode.SESSION_REVIEW_FINALIZED, assertThrows(AppException.class,
                () -> service.rejectQuestions(new RejectQuestionsRequest(
                        List.of(101L), QuestionRejectionCode.DUPLICATE, null))).getResponseCode());
        assertEquals(ResponseCode.SESSION_REVIEW_FINALIZED, assertThrows(AppException.class,
                () -> service.editQuestion(101L, EditQuestionRequest.builder().content("Sửa").build())).getResponseCode());
        verify(sessionRepository, times(3)).lockReviewSessions(List.of(30L), false);
        verify(questionRepository, never()).saveAll(anyList());
        verify(questionRepository, never()).save(any(Question.class));
    }

    private Question questionAssignedTo(Long questionId, Long lecturerId) {
        User lecturer = User.builder().id(lecturerId).build();
        CourseClass courseClass = CourseClass.builder().id(20L).lecturer(lecturer).build();
        Session session = Session.builder().id(30L).courseClass(courseClass).status(SessionStatus.REVIEWING).build();
        return Question.builder()
                .id(questionId)
                .session(session)
                .status(QuestionStatus.PENDING)
                .displayOrder(1)
                .build();
    }

    private void setCurrentUser(Long userId, UserRole role) {
        JwtPayload payload = JwtPayload.builder()
                .userId(userId)
                .role(role.name())
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("test", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
