package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.dto.UpdateGroupInfoDto;
import com.frozenheart.backend.modules.conversation.dto.UpdateMemberRoleRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupDetailsChangedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupMemberRemovedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupRolesChangedDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.GroupInvitationRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.impl.ConversationServiceImpl;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemberRoleTest {
    private static final long GROUP_ID = 10L;
    private static final long CHIEF_ID = 1L;
    private static final long MEMBER_ID = 2L;
    private final Conversation group = Conversation.builder().id(GROUP_ID).name("Tên cũ")
            .type(ConversationType.GROUP).build();

    @Mock private ConversationRepository conversations;
    @Mock private UserParticipantRepository participants;
    @Mock private GroupInvitationRepository invitations;
    @Mock private HiddenChatAccess hiddenChatAccess;
    @Mock private ApplicationEventPublisher events;
    @Mock private GroupActivityWriter activity;
    @Mock private UserProfileRepository profiles;
    @InjectMocks private ConversationServiceImpl service;

    @BeforeEach
    void authenticateChief() {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(CHIEF_ID).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(conversations.findForMembershipUpdate(GROUP_ID)).thenReturn(Optional.of(group));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = ConversationRole.class, names = {"VILLAGE_ELDER", "CHIEF"})
    void publishesRoleUpdateAfterSavingMembers(ConversationRole role) {
        var chief = UserParticipant.builder().role(ConversationRole.CHIEF).build();
        var member = UserParticipant.builder().role(ConversationRole.VILLAGER).build();
        when(participants.findByUserIdAndConversationId(CHIEF_ID, GROUP_ID)).thenReturn(Optional.of(chief));
        when(participants.findByUserIdAndConversationId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));

        service.updateMemberRole(GROUP_ID, MEMBER_ID, new UpdateMemberRoleRequestDto(role));

        assertEquals(role, member.getRole());
        assertEquals(role == ConversationRole.CHIEF ? ConversationRole.VILLAGE_ELDER : ConversationRole.CHIEF, chief.getRole());
        var order = inOrder(conversations, participants, activity, events);
        order.verify(conversations).findForMembershipUpdate(GROUP_ID);
        order.verify(participants).findByUserIdAndConversationId(CHIEF_ID, GROUP_ID);
        if (role == ConversationRole.CHIEF) order.verify(participants).save(chief);
        order.verify(participants).save(member);
        order.verify(activity).roleChanged(group, CHIEF_ID, MEMBER_ID, role);
        order.verify(events).publishEvent(new WsGroupRolesChangedDto(GROUP_ID));
    }

    @Test
    void doesNotPublishWhenFormerChiefTriesToChangeAnotherRole() {
        var formerChief = UserParticipant.builder().role(ConversationRole.VILLAGE_ELDER).build();
        when(participants.findByUserIdAndConversationId(CHIEF_ID, GROUP_ID)).thenReturn(Optional.of(formerChief));

        assertThrows(AppException.class, () ->
                service.updateMemberRole(GROUP_ID, MEMBER_ID, new UpdateMemberRoleRequestDto(ConversationRole.CHIEF)));

        verifyNoInteractions(events, activity);
    }

    @Test
    void recordsRemovalBeforePublishingTheEvent() {
        var chief = UserParticipant.builder().role(ConversationRole.CHIEF).build();
        var member = UserParticipant.builder().role(ConversationRole.VILLAGER).build();
        when(participants.findByUserIdAndConversationId(CHIEF_ID, GROUP_ID)).thenReturn(Optional.of(chief));
        when(participants.findByUserIdAndConversationId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));

        service.removeGroupMember(GROUP_ID, MEMBER_ID);

        assertNotNull(member.getLeftAt());
        assertEquals(Boolean.TRUE, member.getRemovedFromGroup());
        var order = inOrder(participants, invitations, activity, events);
        order.verify(participants).save(member);
        order.verify(invitations).deleteForInviter(GROUP_ID, MEMBER_ID);
        order.verify(activity).memberRemoved(group, CHIEF_ID, MEMBER_ID);
        order.verify(events).publishEvent(new WsGroupMemberRemovedDto(GROUP_ID, MEMBER_ID));
    }

    @Test
    void recordsRenameOnceWhenNameChanges() {
        var chief = UserParticipant.builder().role(ConversationRole.CHIEF).build();
        when(participants.findByUserIdAndConversationId(CHIEF_ID, GROUP_ID)).thenReturn(Optional.of(chief));
        when(conversations.save(group)).thenReturn(group);
        var request = new UpdateGroupInfoDto();
        request.setName("Tên mới");

        service.updateGroupInfo(GROUP_ID, request);
        service.updateGroupInfo(GROUP_ID, request);

        assertEquals("Tên mới", group.getName());
        verify(activity).renamed(group, CHIEF_ID);
        verify(events).publishEvent(new WsGroupDetailsChangedDto(GROUP_ID));
    }

    @Test
    void recordsChiefLeavingWithSuccessorInOneActivity() {
        var chief = UserParticipant.builder().role(ConversationRole.CHIEF).build();
        var member = UserParticipant.builder().role(ConversationRole.VILLAGER).joinedAt(Instant.now())
                .user(User.builder().id(MEMBER_ID).email("member@example.test").build()).build();
        when(participants.findByUserIdAndConversationId(CHIEF_ID, GROUP_ID)).thenReturn(Optional.of(chief));
        when(participants.findByConversationIdAndLeftAtIsNull(GROUP_ID)).thenReturn(List.of(member));

        var result = service.leaveGroup(GROUP_ID, null);

        assertNotNull(chief.getLeftAt());
        assertEquals(MEMBER_ID, result.getNewChief().getUserId());
        assertEquals(ConversationRole.CHIEF, member.getRole());
        verify(activity).memberLeft(group, CHIEF_ID, MEMBER_ID);
        verify(events).publishEvent(new WsGroupDetailsChangedDto(GROUP_ID, CHIEF_ID));
        verify(events).publishEvent(new WsGroupRolesChangedDto(GROUP_ID));
    }
}
