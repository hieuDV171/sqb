package com.frozenheart.backend.modules.conversation.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.conversation.MessageType;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.conversation.dto.ws.SendWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsMessageBroadcastDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserDeletedMessageOnlyMeRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.HiddenChatAccess;
import com.frozenheart.backend.modules.conversation.service.ChatRealtimePublisher;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketReplyTest {

    @Mock ChatRealtimePublisher realtimePublisher;
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock UserDeletedMessageOnlyMeRepository userDeletedMessageOnlyMeRepository;
    @Mock UserParticipantRepository userParticipantRepository;
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock BlockRepository blockRepository;
    @Mock MediaService mediaService;
    @Mock HiddenChatAccess hiddenChatAccess;

    @InjectMocks ChatWebSocketServiceImpl service;

    @Test
    void broadcastsQuoteImmediatelyAndOmitsItsContentForTheViewerWhoDeletedIt() {
        var conversation = Conversation.builder().id(10L).type(ConversationType.DIRECT).build();
        var author = User.builder().id(1L).email("author@example.com").build();
        var replier = User.builder().id(2L).email("replier@example.com").build();
        var authorParticipant = UserParticipant.builder().conversation(conversation).user(author).build();
        var replierParticipant = UserParticipant.builder().conversation(conversation).user(replier).build();
        var original = Message.builder().id(20L).conversation(conversation).sender(author)
                .messageType(MessageType.TEXT).content("Tin nhắn gốc").build();
        var request = new SendWsMessageRequestDto();
        request.setConversationId(10L);
        request.setContent("Phản hồi");
        request.setReplyToMessageId(20L);

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(userParticipantRepository.findByUserIdAndConversationId(2L, 10L)).thenReturn(Optional.of(replierParticipant));
        when(userParticipantRepository.findByConversationIdAndLeftAtIsNull(10L))
                .thenReturn(List.of(authorParticipant, replierParticipant));
        when(userRepository.findById(2L)).thenReturn(Optional.of(replier));
        when(messageRepository.findReplyTarget(20L, 10L)).thenReturn(Optional.of(original));
        when(messageRepository.save(any(Message.class))).thenAnswer(call -> {
            Message saved = call.getArgument(0);
            saved.setId(21L);
            return saved;
        });
        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(UserProfile.builder().fullName("Người gửi gốc").build()));
        when(userProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(userDeletedMessageOnlyMeRepository.findViewersWhoDeleted(20L, List.of(1L, 2L))).thenReturn(Set.of(1L));

        service.handleSendMessage(JwtPayload.builder().userId(2L).build(), request);

        var event = ArgumentCaptor.forClass(WsMessageBroadcastDto.class);
        verify(realtimePublisher).deliver(eq(replierParticipant), event.capture());
        assertEquals(21L, event.getValue().getMessageId());
        assertEquals(20L, event.getValue().getReplyToMessage().getMessageId());
        assertEquals("Người gửi gốc", event.getValue().getReplyToMessage().getSenderName());
        assertEquals("Tin nhắn gốc", event.getValue().getReplyToMessage().getContentPreview());
        assertFalse(event.getValue().getReplyToMessage().isUnavailable());

        verify(realtimePublisher).deliver(eq(authorParticipant), event.capture());
        assertTrue(event.getValue().getReplyToMessage().isUnavailable());
        assertNull(event.getValue().getReplyToMessage().getSenderName());
        assertEquals("Tin nhắn gốc không còn hiển thị", event.getValue().getReplyToMessage().getContentPreview());
    }
}
