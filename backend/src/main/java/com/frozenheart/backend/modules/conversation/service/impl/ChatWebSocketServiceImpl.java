package com.frozenheart.backend.modules.conversation.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.conversation.*;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;
import com.frozenheart.backend.modules.conversation.dto.ReplyMessagePreviewDto;
import com.frozenheart.backend.modules.conversation.dto.ws.*;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserDeletedMessageOnlyMeRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.ChatWebSocketService;
import com.frozenheart.backend.modules.conversation.service.ChatRealtimePublisher;
import com.frozenheart.backend.modules.conversation.service.HiddenChatAccess;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketServiceImpl implements ChatWebSocketService {

    private final ChatRealtimePublisher realtimePublisher;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserDeletedMessageOnlyMeRepository userDeletedMessageOnlyMeRepository;
    private final UserParticipantRepository userParticipantRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BlockRepository blockRepository;
    private final MediaService mediaService;
    private final HiddenChatAccess hiddenChatAccess;

    @Override
    @Transactional
    public void handleSendMessage(JwtPayload session, SendWsMessageRequestDto request) {
        Long currentUserId = session.getUserId();
        if (request == null || request.getConversationId() == null) {
            return;
        }

        Conversation conversation = conversationRepository.findById(request.getConversationId()).orElse(null);
        if (conversation == null) {
            log.warn("[ChatWebSocketServiceImpl] Không tìm thấy cuộc hội thoại: {}", request.getConversationId());
            return;
        }

        UserParticipant senderParticipant = userParticipantRepository
                .findByUserIdAndConversationId(currentUserId, conversation.getId()).orElse(null);
        if (!canAccess(senderParticipant, session)) {
            log.warn("[ChatWebSocketServiceImpl] User {} không còn hoạt động trong cuộc hội thoại {}", currentUserId,
                    conversation.getId());
            return;
        }

        List<UserParticipant> allParticipants = userParticipantRepository
                .findByConversationIdAndLeftAtIsNull(conversation.getId());

        // Nếu là chat DIRECT 1-1, kiểm tra quan hệ chặn (Block)
        if (conversation.getType() == ConversationType.DIRECT) {
            UserParticipant otherParticipant = allParticipants.stream()
                    .filter(p -> !Objects.equals(p.getUser().getId(), currentUserId))
                    .findFirst()
                    .orElse(null);

            if (otherParticipant != null
                    && blockRepository.isBlockedBetween(currentUserId, otherParticipant.getUser().getId())) {
                log.warn("[ChatWebSocketServiceImpl] Không thể gửi tin nhắn trong hội thoại {}: mối quan hệ đã bị chặn",
                        conversation.getId());
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
        Message replyToMessage = request.getReplyToMessageId() == null ? null
                : messageRepository.findReplyTarget(request.getReplyToMessageId(), conversation.getId()).orElse(null);

        Instant now = Instant.now();
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

        ReplyMessagePreviewDto replyPreview = null;
        Set<Long> viewersWithoutOriginal = Set.of();
        if (replyToMessage != null) {
            User replySender = replyToMessage.getSender();
            UserProfile replyProfile = Objects.equals(replySender.getId(), currentUserId) ? senderProfile
                    : userProfileRepository.findByUserId(replySender.getId()).orElse(null);
            String replySenderName = replyProfile != null && replyProfile.getFullName() != null
                    ? replyProfile.getFullName() : replySender.getEmail();
            replyPreview = ReplyMessagePreviewDto.fromMessage(replyToMessage, replySenderName);
            List<Long> viewerIds = allParticipants.stream().map(participant -> participant.getUser().getId()).toList();
            if (!viewerIds.isEmpty()) {
                viewersWithoutOriginal = userDeletedMessageOnlyMeRepository.findViewersWhoDeleted(replyToMessage.getId(), viewerIds);
            }
        }

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
                .replyToMessage(replyPreview)
                .createdAt(savedMessage.getCreatedAt())
                .isEdited(false)
                .build();

        // Broadcast tới queue cá nhân /user/queue/messages của từng thành viên
        WsMessageBroadcastDto redacted = viewersWithoutOriginal.isEmpty() ? broadcastDto : broadcastDto.toBuilder()
                .replyToMessage(ReplyMessagePreviewDto.unavailable(broadcastDto.getReplyToMessageId())).build();
        for (UserParticipant participant : allParticipants) {
            realtimePublisher.deliver(participant, viewersWithoutOriginal.contains(participant.getUser().getId()) ? redacted : broadcastDto);
        }
    }

    @Override
    @Transactional
    public void handleReadMessage(JwtPayload session, ReadWsMessageRequestDto request) {
        Long currentUserId = session.getUserId();
        if (request == null || request.getConversationId() == null || request.getMessageId() == null) {
            return;
        }

        UserParticipant participant = userParticipantRepository
                .findByUserIdAndConversationId(currentUserId, request.getConversationId()).orElse(null);
        if (!canAccess(participant, session)) {
            return;
        }

        Message message = messageRepository.findById(request.getMessageId()).orElse(null);
        if (message == null || !Objects.equals(message.getConversation().getId(), request.getConversationId())) {
            return;
        }

        Instant now = Instant.now();
        participant.setLastReadMessage(message);
        participant.setLastMessageReadAt(now);
        userParticipantRepository.save(participant);

        UserProfile readerProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        UserSummaryDto readerSummary = UserSummaryDto.builder()
                .userId(currentUserId)
                .fullName(readerProfile != null ? readerProfile.getFullName()
                        : (currentUser != null ? currentUser.getEmail() : ""))
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

        otherParticipants.forEach(recipient -> realtimePublisher.deliver(recipient, broadcastDto));
    }

    @Override
    @Transactional(readOnly = true)
    public void handleTyping(JwtPayload session, TypingWsRequestDto request) {
        if (request == null || request.conversationId() == null) {
            return;
        }
        Long currentUserId = session.getUserId();
        List<UserParticipant> participants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(request.conversationId());
        UserParticipant sender = participants.stream()
                .filter(participant -> Objects.equals(participant.getUser().getId(), currentUserId))
                .findFirst().orElse(null);
        if (!canAccess(sender, session)) {
            return;
        }

        UserProfile userProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        User currentUser = sender.getUser();

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .userId(currentUserId)
                .fullName(userProfile != null && userProfile.getFullName() != null
                        ? userProfile.getFullName() : currentUser.getEmail())
                .avatarUrl(userProfile != null ? userProfile.getAvatarUrl() : null)
                .frameUrl(userProfile != null ? userProfile.getAvatarFrameUrl() : null)
                .build();

        var broadcastDto = new WsTypingBroadcastDto(ChatWsEventType.TYPING_STATUS,
                request.conversationId(), userSummary, request.typing(), Instant.now());

        participants.stream().filter(participant -> participant != sender)
                .forEach(participant -> realtimePublisher.deliver(participant, broadcastDto));
    }

    private boolean canAccess(UserParticipant participant, JwtPayload session) {
        return participant != null && participant.getLeftAt() == null
                && (participant.getHiddenAt() == null || hiddenChatAccess.isUnlocked(session));
    }

}
