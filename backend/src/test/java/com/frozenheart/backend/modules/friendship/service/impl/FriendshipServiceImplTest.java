package com.frozenheart.backend.modules.friendship.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipId;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import com.frozenheart.backend.modules.follow.service.FollowService;
import com.frozenheart.backend.modules.friendship.dto.AcceptFriendRequestDto;
import com.frozenheart.backend.modules.friendship.dto.SendFriendRequestDto;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceImplTest {
    @Mock FriendshipRepository repository;
    @Mock BlockRepository blockRepository;
    @Mock UserRepository userRepository;
    @Mock FollowService followService;
    @Mock CounterMetricsService counterMetricsService;
    @Mock BadgeService badgeService;
    @InjectMocks FriendshipServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void senderCanCancelPendingRequest() {
        authenticate(1L);
        Friendship row = request(1L, 2L, FriendshipStatus.PENDING);
        when(repository.findFriendshipsBetweenForUpdate(1L, 2L)).thenReturn(Optional.of(row));

        service.cancelSentRequest(2L);

        verify(repository).delete(row);
        verifyNoInteractions(followService);
    }

    @Test
    void receiverCannotCancelSomeoneElsesRequest() {
        authenticate(2L);
        when(repository.findFriendshipsBetweenForUpdate(2L, 1L))
                .thenReturn(Optional.of(request(1L, 2L, FriendshipStatus.PENDING)));

        var failure = assertThrows(AppException.class, () -> service.cancelSentRequest(1L));

        assertEquals(ResponseCode.RESOURCE_NOT_FOUND, failure.getResponseCode());
        verify(repository, never()).delete(any(Friendship.class));
    }

    @Test
    void cancellationCannotRemoveAcceptedFriendship() {
        authenticate(1L);
        when(repository.findFriendshipsBetweenForUpdate(1L, 2L))
                .thenReturn(Optional.of(request(1L, 2L, FriendshipStatus.ACCEPTED)));

        var failure = assertThrows(AppException.class, () -> service.cancelSentRequest(2L));

        assertEquals(ResponseCode.ALREADY_FRIENDS, failure.getResponseCode());
        verify(repository, never()).delete(any(Friendship.class));
    }

    @Test
    void receiverCannotAcceptARequestThatWasWithdrawn() {
        authenticate(2L);
        when(repository.findFriendshipsBetweenForUpdate(1L, 2L)).thenReturn(Optional.empty());
        AcceptFriendRequestDto request = new AcceptFriendRequestDto();
        request.setRequesterId(1L);

        var failure = assertThrows(AppException.class, () -> service.acceptFriendRequest(request));

        assertEquals(ResponseCode.RESOURCE_NOT_FOUND, failure.getResponseCode());
        verify(repository, never()).save(any(Friendship.class));
    }

    @Test
    void sendingToSameRoleAutomaticallyFollowsRecipient() {
        authenticate(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.STUDENT)));
        when(repository.save(any(Friendship.class))).thenAnswer(call -> call.getArgument(0));
        var request = new SendFriendRequestDto();
        request.setAddresseeId(2L);

        var response = service.sendFriendRequest(request);

        assertEquals(FriendshipStatus.PENDING, response.getStatus());
        verify(followService).followUser(2L);
    }

    @Test
    void acceptanceFollowsSenderInTheOtherDirection() {
        authenticate(2L);
        Friendship row = request(1L, 2L, FriendshipStatus.PENDING);
        row.setSender(user(1L, UserRole.LECTURER));
        row.setReceiver(user(2L, UserRole.LECTURER));
        when(repository.findFriendshipsBetweenForUpdate(1L, 2L)).thenReturn(Optional.of(row));
        when(repository.save(row)).thenReturn(row);
        var request = new AcceptFriendRequestDto();
        request.setRequesterId(1L);

        var response = service.acceptFriendRequest(request);

        assertEquals(FriendshipStatus.ACCEPTED, response.getStatus());
        verify(followService).followUser(1L);
    }

    @Test
    void sendingToAnotherRoleIsRejectedBeforeCreatingEitherRelationship() {
        authenticate(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.LECTURER)));
        var request = new SendFriendRequestDto();
        request.setAddresseeId(2L);

        var failure = assertThrows(AppException.class, () -> service.sendFriendRequest(request));

        assertEquals(ResponseCode.ACTION_NOT_ALLOWED, failure.getResponseCode());
        verifyNoInteractions(repository, followService);
    }

    @Test
    void acceptingARequestBetweenDifferentRolesIsRejected() {
        authenticate(2L);
        Friendship row = request(1L, 2L, FriendshipStatus.PENDING);
        row.setSender(user(1L, UserRole.STUDENT));
        row.setReceiver(user(2L, UserRole.LECTURER));
        when(repository.findFriendshipsBetweenForUpdate(1L, 2L)).thenReturn(Optional.of(row));
        var request = new AcceptFriendRequestDto();
        request.setRequesterId(1L);

        var failure = assertThrows(AppException.class, () -> service.acceptFriendRequest(request));

        assertEquals(ResponseCode.ACTION_NOT_ALLOWED, failure.getResponseCode());
        assertEquals(FriendshipStatus.PENDING, row.getStatus());
        verify(repository, never()).save(any(Friendship.class));
        verifyNoInteractions(followService);
    }

    private User user(Long id, UserRole role) {
        return User.builder().id(id).role(role).build();
    }

    private Friendship request(Long sender, Long receiver, FriendshipStatus status) {
        return Friendship.builder().id(new FriendshipId(sender, receiver)).status(status).build();
    }

    private void authenticate(Long id) {
        var authentication = new UsernamePasswordAuthenticationToken("test", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(id).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
