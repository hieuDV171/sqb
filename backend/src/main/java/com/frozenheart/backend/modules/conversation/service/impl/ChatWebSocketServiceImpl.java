package com.frozenheart.backend.modules.conversation.service.impl;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.conversation.*;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;
import com.frozenheart.backend.modules.conversation.dto.ws.*;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.ChatWebSocketService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketServiceImpl implements ChatWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserParticipantRepository userParticipantRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BlockRepository blockRepository;
    private final MediaService mediaService;

    @Override
    @Transactional
    public void handleSendMessage(Long currentUserId, SendWsMessageRequestDto request) {
        if (request == null || request.getConversationId() == null) {
            return;
        }

        Conversation conversation = conversationRepository.findById(request.getConversationId()).orElse(null);
        if (conversation == null) {
            log.warn("[ChatWebSocketServiceImpl] Conversation not found: {}", request.getConversationId());
            return;
        }

        UserParticipant senderParticipant = userParticipantRepository
                .findByUserIdAndConversationId(currentUserId, conversation.getId()).orElse(null);
        if (senderParticipant == null || senderParticipant.getLeftAt() != null) {
            log.warn("[ChatWebSocketServiceImpl] User {} is not an active participant in conversation {}", currentUserId, conversation.getId());
            return;
        }

        List<UserParticipant> allParticipants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversation.getId());

        // Nếu là chat DIRECT 1-1, kiểm tra quan hệ chặn (Block)
        if (conversation.getType() == ConversationType.DIRECT) {
            UserParticipant otherParticipant = allParticipants.stream()
                    .filter(p -> !Objects.equals(p.getUser().getId(), currentUserId))
                    .findFirst()
                    .orElse(null);

            if (otherParticipant != null && blockRepository.isBlockedBetween(currentUserId, otherParticipant.getUser().getId())) {
                log.warn("[ChatWebSocketServiceImpl] Cannot send message in conversation {}: blocked relationship", conversation.getId());
                return;
            }
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null) {
            return;
        }

        // Xử lý media URLs bằng static factory MediaItem.of
        List<MediaItem> mediaItems = null;
        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            List<String> validUrls = request.getMediaUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .toList();

            if (!validUrls.isEmpty()) {
                mediaService.confirmMediaPermanent(validUrls);
                mediaItems = validUrls.stream()
                        .map(MediaItem::of)
                        .collect(Collectors.toList());
            }
        }

        // Xử lý reply
        Message replyToMessage = null;
        if (request.getReplyToMessageId() != null) {
            replyToMessage = messageRepository.findById(request.getReplyToMessageId()).orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();
        MessageType msgType = request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT;

        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(request.getContent())
                .messageType(msgType)
                .mediaUrls(mediaItems)
                .replyToMessage(replyToMessage)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Message savedMessage = messageRepository.save(message);

        // Cập nhật cuộc hội thoại
        conversation.setLastMessage(savedMessage);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        // Cập nhật người gửi đã đọc tin nhắn của chính mình
        senderParticipant.setLastReadMessage(savedMessage);
        senderParticipant.setLastMessageReadAt(now);
        userParticipantRepository.save(senderParticipant);

        // Lấy profile người gửi để broadcast dùng UserSummaryDto
        UserProfile senderProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        UserSummaryDto senderSummary = UserSummaryDto.builder()
                .userId(currentUserId)
                .fullName(senderProfile != null ? senderProfile.getFullName() : currentUser.getEmail())
                .avatarUrl(senderProfile != null ? senderProfile.getAvatarUrl() : null)
                .frameUrl(senderProfile != null ? senderProfile.getAvatarFrameUrl() : null)
                .build();

        List<String> mediaUrlStrings = (mediaItems != null)
                ? mediaItems.stream().map(MediaItem::url).toList()
                : Collections.emptyList();

        WsMessageBroadcastDto broadcastDto = WsMessageBroadcastDto.builder()
                .eventType(ChatWsEventType.NEW_MESSAGE)
                .messageId(savedMessage.getId())
                .conversationId(conversation.getId())
                .conversationType(conversation.getType())
                .sender(senderSummary)
                .content(savedMessage.getContent())
                .messageType(savedMessage.getMessageType())
                .mediaUrls(mediaUrlStrings)
                .replyToMessageId(replyToMessage != null ? replyToMessage.getId() : null)
                .createdAt(savedMessage.getCreatedAt())
                .isEdited(false)
                .build();

        // Broadcast tới queue cá nhân /user/queue/messages của từng thành viên
        for (UserParticipant p : allParticipants) {
            String recipientUserId = p.getUser().getId().toString();
            messagingTemplate.convertAndSendToUser(recipientUserId, "/queue/messages", broadcastDto);
        }
    }

    @Override
    @Transactional
    public void handleReadMessage(Long currentUserId, ReadWsMessageRequestDto request) {
        if (request == null || request.getConversationId() == null || request.getMessageId() == null) {
            return;
        }

        UserParticipant participant = userParticipantRepository
                .findByUserIdAndConversationId(currentUserId, request.getConversationId()).orElse(null);
        if (participant == null || participant.getLeftAt() != null) {
            return;
        }

        Message message = messageRepository.findById(request.getMessageId()).orElse(null);
        if (message == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        participant.setLastReadMessage(message);
        participant.setLastMessageReadAt(now);
        userParticipantRepository.save(participant);

        UserProfile readerProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        UserSummaryDto readerSummary = UserSummaryDto.builder()
                .userId(currentUserId)
                .fullName(readerProfile != null ? readerProfile.getFullName() : (currentUser != null ? currentUser.getEmail() : ""))
                .avatarUrl(readerProfile != null ? readerProfile.getAvatarUrl() : null)
                .frameUrl(readerProfile != null ? readerProfile.getAvatarFrameUrl() : null)
                .build();

        WsReadReceiptBroadcastDto broadcastDto = WsReadReceiptBroadcastDto.builder()
                .eventType(ChatWsEventType.MESSAGE_READ)
                .conversationId(request.getConversationId())
                .messageId(request.getMessageId())
                .reader(readerSummary)
                .readAt(now)
                .build();

        List<UserParticipant> otherParticipants = userParticipantRepository
                .findByConversationIdAndLeftAtIsNull(request.getConversationId()).stream()
                .filter(p -> !Objects.equals(p.getUser().getId(), currentUserId))
                .toList();

        for (UserParticipant p : otherParticipants) {
            String recipientUserId = p.getUser().getId().toString();
            messagingTemplate.convertAndSendToUser(recipientUserId, "/queue/messages", broadcastDto);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void handleTyping(Long currentUserId, TypingWsRequestDto request) {
        if (request == null || request.getConversationId() == null) {
            return;
        }

        UserProfile userProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .userId(currentUserId)
                .fullName(userProfile != null ? userProfile.getFullName() : (currentUser != null ? currentUser.getEmail() : ""))
                .avatarUrl(userProfile != null ? userProfile.getAvatarUrl() : null)
                .frameUrl(userProfile != null ? userProfile.getAvatarFrameUrl() : null)
                .build();

        WsTypingBroadcastDto broadcastDto = WsTypingBroadcastDto.builder()
                .eventType(ChatWsEventType.TYPING_STATUS)
                .conversationId(request.getConversationId())
                .user(userSummary)
                .isTyping(request.isTyping())
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast tới topic của cuộc hội thoại: /topic/conv.{id}.typing
        String destination = "/topic/conv." + request.getConversationId() + ".typing";
        messagingTemplate.convertAndSend(destination, broadcastDto);
    }
}
