package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupMemberRemovedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupMembersAddedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupRolesChangedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsMessageRevokedDto;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatRealtimePublisher {

    private static final String QUEUE = "/queue/messages";

    private final SimpMessagingTemplate messagingTemplate;
    private final UserParticipantRepository userParticipantRepository;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void publishRevocation(WsMessageRevokedDto event) {
        deliverToActiveMembers(event.conversationId(), event);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void publishGroupRolesChanged(WsGroupRolesChangedDto event) {
        deliverToActiveMembers(event.conversationId(), event);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void publishGroupMembersAdded(WsGroupMembersAddedDto event) {
        deliverToActiveMembers(event.conversationId(), event);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void publishGroupMemberRemoved(WsGroupMemberRemovedDto event) {
        deliverToActiveMembers(event.conversationId(), event);
        userParticipantRepository.findByUserIdAndConversationId(event.removedUserId(), event.conversationId())
                .filter(member -> member.getLeftAt() != null && Boolean.TRUE.equals(member.getRemovedFromGroup()))
                .ifPresent(member -> deliver(event.removedUserId(), member.getHiddenAt() != null, event));
    }

    private void deliverToActiveMembers(Long conversationId, Object event) {
        userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversationId)
                .forEach(participant -> deliver(participant, event));
    }

    public void deliver(UserParticipant participant, Object event) {
        deliver(participant.getUser().getId(), participant.getHiddenAt() != null, event);
    }

    private void deliver(Long userId, boolean hidden, Object event) {
        if (!hidden) {
            messagingTemplate.convertAndSendToUser(userId.toString(), QUEUE, event);
            return;
        }
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setNativeHeader(HiddenChatOutboundGuard.HIDDEN_CHAT_HEADER, "true");
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(userId.toString(), QUEUE, event, headers.getMessageHeaders());
    }
}
