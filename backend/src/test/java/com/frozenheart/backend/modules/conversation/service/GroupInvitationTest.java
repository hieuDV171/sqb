package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.GroupInvitation;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.dto.AddGroupMembersRequestDto;
import com.frozenheart.backend.modules.conversation.dto.CreateGroupConversationDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupMembersAddedDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.GroupInvitationRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.impl.ConversationServiceImpl;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupInvitationTest {
    private static final Long GROUP_ID = 100L;
    private static final Long INVITATION_ID = 200L;
    private final User inviter = User.builder().id(1L).active(true).build();
    private final User invitee = User.builder().id(2L).active(true).build();
    private final User friend = User.builder().id(3L).active(true).build();
    private final Conversation group = Conversation.builder().id(GROUP_ID).type(ConversationType.GROUP).build();

    @Mock private GroupInvitationRepository invitations;
    @Mock private ConversationRepository conversations;
    @Mock private UserParticipantRepository participants;
    @Mock private BlockRepository blocks;
    @Mock private UserProfileRepository profiles;
    @Mock private UserRepository users;
    @Mock private HiddenChatAccess hiddenChatAccess;
    @Mock private ApplicationEventPublisher events;
    @Mock private GroupInvitationService connections;
    @Captor private ArgumentCaptor<List<GroupInvitation>> savedInvitations;
    @Captor private ArgumentCaptor<List<UserParticipant>> savedParticipants;
    @Captor private ArgumentCaptor<UserParticipant> acceptedParticipant;
    @InjectMocks private GroupInvitationService invitationService;
    @InjectMocks private ConversationServiceImpl conversationService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creatingGroupAddsFriendAndInvitesFollowerWithoutGrantingMembership() {
        authenticate(inviter);
        List<Long> targets = List.of(invitee.getId(), friend.getId());
        var request = new CreateGroupConversationDto();
        request.setName("Ôn tập");
        request.setMemberIds(targets);
        when(users.findById(inviter.getId())).thenReturn(Optional.of(inviter));
        when(users.findAllById(targets)).thenReturn(List.of(invitee, friend));
        when(connections.connections(inviter.getId(), targets)).thenReturn(
                new GroupInvitationService.Connections(Set.of(friend.getId()), Set.of(), Set.of(invitee.getId())));
        when(conversations.save(any(Conversation.class))).thenReturn(group);

        assertEquals(1, conversationService.createGroupConversation(request).getInvitedMemberCount());

        verify(participants).saveAll(savedParticipants.capture());
        assertEquals(List.of(inviter.getId(), friend.getId()), savedParticipants.getValue().stream()
                .map(member -> member.getUser().getId()).toList());
        verify(invitations).saveAll(savedInvitations.capture());
        assertEquals(List.of(invitee.getId()), savedInvitations.getValue().stream()
                .map(invitation -> invitation.getInvitee().getId()).toList());
    }

    @Test
    void addingToExistingGroupInvitesFollowedAccount() {
        authenticate(inviter);
        List<Long> targets = List.of(invitee.getId());
        var request = AddGroupMembersRequestDto.builder().userIds(targets).build();
        var membership = UserParticipant.builder().user(inviter).role(ConversationRole.CHIEF).build();
        when(conversations.findForMembershipUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(participants.findByUserIdAndConversationId(inviter.getId(), GROUP_ID)).thenReturn(Optional.of(membership));
        when(participants.findByConversationIdAndLeftAtIsNull(GROUP_ID)).thenReturn(List.of(membership));
        when(users.findAllById(targets)).thenReturn(List.of(invitee));
        when(connections.connections(inviter.getId(), targets)).thenReturn(
                new GroupInvitationService.Connections(Set.of(), Set.of(invitee.getId()), Set.of()));

        var result = conversationService.addGroupMembers(GROUP_ID, request);

        assertEquals(1, result.getInvitedMemberCount());
        assertTrue(result.getAddedMembers().isEmpty());
        assertTrue(result.getFailedMembers().isEmpty());
        verify(participants).saveAll(List.of());
        verify(invitations).saveAll(savedInvitations.capture());
        assertSame(invitee, savedInvitations.getValue().getFirst().getInvitee());
        verifyNoInteractions(events);
    }

    @Test
    void addingFriendBackSavesMembershipAndPublishesChange() {
        authenticate(inviter);
        List<Long> targets = List.of(friend.getId());
        var caller = UserParticipant.builder().user(inviter).role(ConversationRole.CHIEF).build();
        var formerMember = UserParticipant.builder().user(friend).role(ConversationRole.VILLAGER)
                .leftAt(Instant.now()).removedFromGroup(true).build();
        when(conversations.findForMembershipUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(participants.findByUserIdAndConversationId(inviter.getId(), GROUP_ID)).thenReturn(Optional.of(caller));
        when(participants.findByConversationIdAndLeftAtIsNull(GROUP_ID)).thenReturn(List.of(caller));
        when(participants.findByConversationIdAndUserIdIn(GROUP_ID, targets)).thenReturn(List.of(formerMember));
        when(users.findAllById(targets)).thenReturn(List.of(friend));
        when(connections.connections(inviter.getId(), targets)).thenReturn(
                new GroupInvitationService.Connections(Set.of(friend.getId()), Set.of(), Set.of()));

        var result = conversationService.addGroupMembers(GROUP_ID, AddGroupMembersRequestDto.builder().userIds(targets).build());

        assertEquals(friend.getId(), result.getAddedMembers().getFirst().getUserId());
        assertNull(formerMember.getLeftAt());
        assertNull(formerMember.getRemovedFromGroup());
        var order = inOrder(participants, events);
        order.verify(participants).saveAll(List.of(formerMember));
        order.verify(events).publishEvent(new WsGroupMembersAddedDto(GROUP_ID));
    }

    @Test
    void acceptingCreatesMembershipAndConsumesInvitation() {
        authenticate(invitee);
        GroupInvitation invitation = givenInvitation(Instant.now().plusSeconds(60));
        when(participants.existsByUserIdAndConversationIdAndLeftAtIsNull(inviter.getId(), GROUP_ID)).thenReturn(true);

        assertEquals(GROUP_ID, invitationService.accept(INVITATION_ID));

        verify(participants).save(acceptedParticipant.capture());
        assertSame(invitee, acceptedParticipant.getValue().getUser());
        assertEquals(ConversationRole.VILLAGER, acceptedParticipant.getValue().getRole());
        assertNull(acceptedParticipant.getValue().getLeftAt());
        verify(invitations).delete(invitation);
        verify(events).publishEvent(new WsGroupMembersAddedDto(GROUP_ID));
    }

    @Test
    void otherAccountCannotAcceptOrDeclineInvitation() {
        authenticate(friend);

        assertThrows(AppException.class, () -> invitationService.accept(INVITATION_ID));
        assertThrows(AppException.class, () -> invitationService.decline(INVITATION_ID));

        verifyNoInteractions(conversations, participants);
        verify(invitations, never()).deleteForInvitee(any(), any());
    }

    @Test
    void decliningConsumesInvitationWithoutGrantingMembership() {
        authenticate(invitee);
        when(invitations.findConversationId(INVITATION_ID, invitee.getId())).thenReturn(Optional.of(GROUP_ID));
        when(conversations.findForMembershipUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(invitations.deleteForInvitee(INVITATION_ID, invitee.getId())).thenReturn(1);

        invitationService.decline(INVITATION_ID);

        verify(invitations).deleteForInvitee(INVITATION_ID, invitee.getId());
        verifyNoInteractions(participants);
        verifyNoInteractions(events);
    }

    @Test
    void expiredInvitationDoesNotGrantMembership() {
        authenticate(invitee);
        givenInvitation(Instant.now().minusSeconds(60));

        assertThrows(AppException.class, () -> invitationService.accept(INVITATION_ID));

        verifyNoInteractions(participants);
    }

    @Test
    void blockedInvitationDoesNotGrantMembership() {
        authenticate(invitee);
        givenInvitation(Instant.now().plusSeconds(60));
        when(participants.existsByUserIdAndConversationIdAndLeftAtIsNull(inviter.getId(), GROUP_ID)).thenReturn(true);
        when(blocks.isBlockedBetween(inviter.getId(), invitee.getId())).thenReturn(true);

        assertThrows(AppException.class, () -> invitationService.accept(INVITATION_ID));

        verify(participants, never()).save(any());
    }

    private GroupInvitation givenInvitation(Instant expiresAt) {
        GroupInvitation invitation = new GroupInvitation(group, inviter, invitee, expiresAt);
        when(invitations.findConversationId(INVITATION_ID, invitee.getId())).thenReturn(Optional.of(GROUP_ID));
        when(conversations.findForMembershipUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(invitations.findForInvitee(INVITATION_ID, invitee.getId())).thenReturn(Optional.of(invitation));
        return invitation;
    }

    private void authenticate(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(user.getId()).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
