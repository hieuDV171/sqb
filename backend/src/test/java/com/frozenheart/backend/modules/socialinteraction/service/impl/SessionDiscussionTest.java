package com.frozenheart.backend.modules.socialinteraction.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.socialinteraction.Comment;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.socialinteraction.dto.CreateCommentRequest;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import com.frozenheart.backend.modules.socialinteraction.service.helper.InteractionTargetValidator;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionDiscussionTest {
    @Mock SessionRepository sessions;
    @Mock CommentRepository comments;
    @Mock UserRepository users;
    @Mock UserProfileRepository profiles;
    @Mock MediaService media;
    @Mock CounterMetricsService counters;
    @Mock ApplicationEventPublisher events;
    private CommentServiceImpl service;
    private final User viewer = User.builder().id(7L).email("viewer@example.com").build();

    @BeforeEach
    void setUp() {
        var authentication = new UsernamePasswordAuthenticationToken("viewer", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(viewer.getId()).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var validator = new InteractionTargetValidator(null, sessions, null);
        service = new CommentServiceImpl(comments, users, profiles, validator, media, counters, events);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void privateSessionRejectsRootReadsRepliesAndWritesFromOtherUsers() {
        when(sessions.findByIdFetchProposer(41L)).thenReturn(Optional.of(Session.builder()
                .id(41L).status(SessionStatus.REVIEWING).proposer(User.builder().id(8L).build()).build()));
        when(comments.findById(50L)).thenReturn(Optional.of(Comment.builder().targetType("SESSION").targetId(41L).build()));

        assertEquals(ResponseCode.ACCESS_DENIED, assertThrows(AppException.class,
                () -> service.getComments(InteractionTargetType.SESSION, 41L, null, null, null, 10)).getResponseCode());
        assertEquals(ResponseCode.ACCESS_DENIED, assertThrows(AppException.class,
                () -> service.getComments(null, null, 50L, null, null, 10)).getResponseCode());
        assertEquals(ResponseCode.ACCESS_DENIED, assertThrows(AppException.class,
                () -> service.createComment(request(41L, null))).getResponseCode());
        verify(comments, never()).save(any());
    }

    @Test
    void resolvedSessionWritesItsOwnCounterAndRejectsRepliesToAnotherTarget() {
        when(sessions.findByIdFetchProposer(41L)).thenReturn(Optional.of(Session.builder().status(SessionStatus.RESOLVED).build()));
        when(users.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(comments.findForModeration(50L)).thenReturn(Optional.of(Comment.builder().targetType("POST").targetId(41L).build()));
        when(comments.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(ResponseCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class,
                () -> service.createComment(request(41L, 50L))).getResponseCode());
        var saved = service.createComment(request(41L, null));

        assertEquals("SESSION", saved.getTargetType());
        assertEquals(41L, saved.getTargetId());
        verify(counters).incrementSessionComments(41L, 1);
        verify(counters, never()).incrementPostComments(anyLong(), eq(1));
    }

    private CreateCommentRequest request(Long targetId, Long parentCommentId) {
        return CreateCommentRequest.builder().targetType(InteractionTargetType.SESSION).targetId(targetId)
                .parentCommentId(parentCommentId).content("Bộ này nên bổ sung một câu ví dụ.").build();
    }
}
