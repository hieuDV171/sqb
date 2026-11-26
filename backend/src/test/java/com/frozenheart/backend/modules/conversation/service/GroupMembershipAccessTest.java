package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.conversation.MessageType;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.impl.ConversationServiceImpl;
import com.frozenheart.backend.modules.conversation.service.impl.MessageServiceImpl;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMembershipAccessTest {
    private static final long GROUP_ID = 10L;
    private static final long MEMBER_ID = 2L;

    @Mock private ConversationRepository conversations;
    @Mock private UserParticipantRepository participants;
    @Mock private MessageRepository messages;
    @Mock private UserProfileRepository profiles;
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

    @Test
    void returnsSystemRowsFromBeforeJoiningUsingTheMessageCursor() {
        var joinedAt = Instant.parse("2026-11-25T03:00:00Z");
        var group = Conversation.builder().id(GROUP_ID).type(ConversationType.GROUP).build();
        var member = UserParticipant.builder().joinedAt(joinedAt).build();
        var oldActivity = Message.builder().id(25L).conversation(group).messageType(MessageType.SYSTEM)
                .content("An đã đổi tên nhóm thành “Ôn tập”").createdAt(joinedAt.minusSeconds(86_400)).build();
        when(conversations.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(participants.findByUserIdAndConversationId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));
        when(messages.findMessagesCursor(GROUP_ID, MEMBER_ID, 30L, PageRequest.of(0, 2)))
                .thenReturn(List.of(oldActivity, Message.builder().id(24L).build()));

        var page = messageService.getMessages(GROUP_ID, 30L, 1);

        var row = page.getMessages().getFirst();
        assertEquals(MessageType.SYSTEM, row.getMessageType());
        assertEquals(oldActivity.getContent(), row.getContent());
        assertNull(row.getSenderId());
        assertEquals(25L, page.getPagination().after());
        assertTrue(page.getPagination().hasNext());
    }

    @Test
    void loadingOnlySystemRowsDoesNotMoveTheReadReceipt() {
        var group = Conversation.builder().id(GROUP_ID).type(ConversationType.GROUP).build();
        var member = UserParticipant.builder().build();
        var activity = Message.builder().id(25L).conversation(group).messageType(MessageType.SYSTEM).build();
        when(conversations.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(participants.findByUserIdAndConversationId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));
        when(messages.findMessagesCursor(GROUP_ID, MEMBER_ID, Long.MAX_VALUE, PageRequest.of(0, 2)))
                .thenReturn(List.of(activity));

        assertEquals(1, messageService.getMessages(GROUP_ID, null, 1).getMessages().size());
        assertNull(member.getLastReadMessage());
        verify(participants, never()).save(member);
    }

    @Test
    void refreshingLatestHistoryReturnsTheExistingReceiptWithoutMarkingLoadedMessagesRead() {
        var group = Conversation.builder().id(GROUP_ID).type(ConversationType.GROUP).build();
        var previous = Message.builder().id(24L).build();
        var member = UserParticipant.builder().lastReadMessage(previous).build();
        var sender = User.builder().id(1L).email("sender@example.com").build();
        var incoming = Message.builder().id(25L).conversation(group).sender(sender).messageType(MessageType.TEXT).build();
        when(conversations.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(participants.findByUserIdAndConversationId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));
        when(messages.findMessagesCursor(GROUP_ID, MEMBER_ID, Long.MAX_VALUE, PageRequest.of(0, 2)))
                .thenReturn(List.of(incoming));

        var page = messageService.getMessages(GROUP_ID, null, 1);

        assertEquals(25L, page.getMessages().getFirst().getMessageId());
        assertEquals(24L, page.getLastReadMessageId());
        assertEquals(previous, member.getLastReadMessage());
        verify(participants, never()).save(member);
    }
}
