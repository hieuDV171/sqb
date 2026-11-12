package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.dto.UpdateMemberRoleRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupRolesChangedDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.impl.ConversationServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemberRoleTest {
    private static final long GROUP_ID = 10L;
    private static final long CHIEF_ID = 1L;
    private static final long MEMBER_ID = 2L;

    @Mock private ConversationRepository conversations;
    @Mock private UserParticipantRepository participants;
    @Mock private HiddenChatAccess hiddenChatAccess;
    @Mock private ApplicationEventPublisher events;
    @InjectMocks private ConversationServiceImpl service;

    @BeforeEach
    void authenticateChief() {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(CHIEF_ID).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(conversations.findForMembershipUpdate(GROUP_ID)).thenReturn(Optional.of(
                Conversation.builder().id(GROUP_ID).type(ConversationType.GROUP).build()));
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
        var order = inOrder(conversations, participants, events);
        order.verify(conversations).findForMembershipUpdate(GROUP_ID);
        order.verify(participants).findByUserIdAndConversationId(CHIEF_ID, GROUP_ID);
        if (role == ConversationRole.CHIEF) order.verify(participants).save(chief);
        order.verify(participants).save(member);
        order.verify(events).publishEvent(new WsGroupRolesChangedDto(GROUP_ID));
    }

    @Test
    void doesNotPublishWhenFormerChiefTriesToChangeAnotherRole() {
        var formerChief = UserParticipant.builder().role(ConversationRole.VILLAGE_ELDER).build();
        when(participants.findByUserIdAndConversationId(CHIEF_ID, GROUP_ID)).thenReturn(Optional.of(formerChief));

        assertThrows(AppException.class, () ->
                service.updateMemberRole(GROUP_ID, MEMBER_ID, new UpdateMemberRoleRequestDto(ConversationRole.CHIEF)));

        verifyNoInteractions(events);
    }
}
