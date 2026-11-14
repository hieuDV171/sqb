package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.socialinteraction.*;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.report.dto.AdminReportDto.*;
import com.frozenheart.backend.modules.report.repository.ModerationActionRepository;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {
    @Mock ReportRepository reports;
    @Mock ModerationActionRepository actions;
    @Mock ReportTargets targets;
    @Mock PostRepository posts;
    @Mock CommentRepository comments;
    @Mock UserRepository users;
    @Mock UserProfileRepository profiles;
    @Mock CounterMetricsService counters;
    @Mock UserDeviceRepository devices;
    @Mock NotificationWriter notifications;
    @Mock ReportEvidenceStorage evidence;
    @Mock R2Properties storageProperties;
    @Mock ApplicationEventPublisher events;
    @InjectMocks AdminReportService service;

    private final User admin = User.builder().id(1L).role(UserRole.ADMIN).build();

    @BeforeEach
    void authenticateAdmin() {
        authenticate("ADMIN");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refusesNonAdminsBeforeReadingQueue() {
        authenticate("STUDENT");
        assertThat(assertThrows(AppException.class, () -> service.queue(null, null, null, null, null))
                .getResponseCode()).isEqualTo(ResponseCode.ACCESS_DENIED);
        verifyNoInteractions(reports);
    }

    @Test
    void staleVersionStopsBeforeTouchingTarget() {
        var report = report("POST");
        when(reports.findForReview(10L)).thenReturn(Optional.of(report));

        assertThat(assertThrows(AppException.class,
                () -> service.resolve(10L, new ResolveRequest(1L, Decision.HIDE_CONTENT, "Spam", null)))
                .getResponseCode()).isEqualTo(ResponseCode.REPORT_CONFLICT);
        verifyNoInteractions(posts, actions);
    }

    @Test
    void anotherReportCannotHideAnAlreadyHiddenPost() {
        var report = report("POST");
        var post = new Post();
        post.setActiveModerationAction(ModerationAction.builder().id(99L).build());
        when(reports.findForReview(10L)).thenReturn(Optional.of(report));
        when(users.getReferenceById(1L)).thenReturn(admin);
        when(posts.findForModeration(40L)).thenReturn(Optional.of(post));

        assertThat(assertThrows(AppException.class,
                () -> service.resolve(10L, new ResolveRequest(0L, Decision.HIDE_CONTENT, "Spam", null)))
                .getResponseCode()).isEqualTo(ResponseCode.REPORT_CONFLICT);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        verifyNoInteractions(actions, notifications);
    }

    @Test
    void hidesAndRestoresCommentWithOneCounterChangeEach() {
        var report = report("COMMENT");
        var comment = Comment.builder().id(40L).targetType("QUESTION").targetId(7L).content("Original").build();
        prepareMutation(report);
        when(comments.findForModeration(40L)).thenReturn(Optional.of(comment));
        when(actions.save(any(ModerationAction.class))).thenAnswer(invocation -> {
            ModerationAction action = invocation.getArgument(0);
            action.setId(action.getType() == ModerationActionType.HIDE_CONTENT ? 90L : 91L);
            return action;
        });
        when(actions.findById(90L)).thenReturn(Optional.of(ModerationAction.builder()
                .type(ModerationActionType.HIDE_CONTENT).report(report).build()));

        service.resolve(10L, new ResolveRequest(0L, Decision.HIDE_CONTENT, "Spam", null));
        assertThat(comment.getActiveModerationAction().getId()).isEqualTo(90L);
        assertThat(comment.getContent()).isEqualTo("Original");
        assertThat(report.getOutcome()).isEqualTo(ReportOutcome.ACTION_TAKEN);
        verify(counters).incrementQuestionComments(7L, -1);

        service.reverse(90L, new ReverseRequest("Đã xem xét lại"));
        assertThat(comment.getActiveModerationAction()).isNull();
        verify(counters).incrementQuestionComments(7L, 1);
        assertThat(assertThrows(AppException.class, () -> service.reverse(90L, new ReverseRequest("Lặp lại")))
                .getResponseCode()).isEqualTo(ResponseCode.REPORT_CONFLICT);
        verify(counters).incrementQuestionComments(7L, 1);
    }

    @Test
    void suspensionRevokesDeviceSessionsAndKeepsAccountActiveFlag() {
        var report = report("USER");
        var student = User.builder().id(40L).role(UserRole.STUDENT).build();
        prepareMutation(report);
        when(users.findByIdForUpdate(40L)).thenReturn(Optional.of(student));
        when(actions.save(any(ModerationAction.class))).thenAnswer(invocation -> {
            ModerationAction action = invocation.getArgument(0);
            action.setId(90L);
            return action;
        });

        service.resolve(10L, new ResolveRequest(0L, Decision.SUSPEND_ACCOUNT, "Quấy rối", 7));

        assertThat(student.isSuspendedAt(Instant.now())).isTrue();
        assertThat(student.getActiveModerationAction().getId()).isEqualTo(90L);
        assertThat(student.isActive()).isTrue();
        assertThat(student.getSessionEpoch()).isEqualTo(1L);
        verify(devices).deactivateForAccounts(List.of(40L));
        verify(events).publishEvent(new com.frozenheart.backend.core.security.AccountWebSocketSessions.AccountSuspended(40L));
    }

    private void prepareMutation(Report report) {
        when(reports.findForReview(10L)).thenReturn(Optional.of(report));
        when(reports.findDetails(10L)).thenReturn(Optional.of(report));
        when(users.getReferenceById(1L)).thenReturn(admin);
    }

    private static Report report(String type) {
        return Report.builder().id(10L).version(0L).targetType(type).targetId(40L)
                .status(ReportStatus.PENDING).build();
    }

    private static void authenticate(String role) {
        var auth = new UsernamePasswordAuthenticationToken("test", null, List.of());
        auth.setDetails(JwtPayload.builder().userId(1L).role(role).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
