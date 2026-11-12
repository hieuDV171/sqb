package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.impl.ConversationServiceImpl;
import com.frozenheart.backend.modules.conversation.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMembershipAccessTest {
    private static final long GROUP_ID = 10L;
    private static final long MEMBER_ID = 2L;

    @Mock private ConversationRepository conversations;
    @Mock private UserParticipantRepository participants;
    @Mock private MessageRepository messages;
    @Mock private HiddenChatAccess hiddenChatAccess;
    @InjectMocks private MessageServiceImpl messageService;
    @InjectMocks private ConversationServiceImpl conversationService;

    @BeforeEach
    void authenticateMember() {
        var authentication = new UsernamePasswordAuthenticationToken("2", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(MEMBER_ID).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = true)
    void reportsRecordedExitReasonWithoutReturningGroupData(Boolean removed) {
        var member = UserParticipant.builder().leftAt(Instant.now()).removedFromGroup(removed).build();
        when(conversations.findById(GROUP_ID)).thenReturn(Optional.of(Conversation.builder().id(GROUP_ID).build()));
        when(participants.findByUserIdAndConversationId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));
        var expected = Boolean.TRUE.equals(removed) ? ResponseCode.GROUP_MEMBER_REMOVED : ResponseCode.GROUP_MEMBER_INACTIVE;

        assertEquals(expected, assertThrows(AppException.class,
                () -> messageService.getMessages(GROUP_ID, null, 20)).getResponseCode());
        assertEquals(expected, assertThrows(AppException.class,
                () -> conversationService.getGroupMembers(GROUP_ID)).getResponseCode());
        verifyNoInteractions(messages);
    }
}
