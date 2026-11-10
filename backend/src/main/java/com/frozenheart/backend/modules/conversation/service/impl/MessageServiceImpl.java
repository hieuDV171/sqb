package com.frozenheart.backend.modules.conversation.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.conversation.*;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.constant.MessageDeleteScope;
import com.frozenheart.backend.modules.conversation.dto.*;
import com.frozenheart.backend.modules.conversation.dto.ws.WsMessageRevokedDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserDeletedMessageOnlyMeRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.MessageService;
import com.frozenheart.backend.modules.conversation.service.HiddenChatAccess;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserParticipantRepository userParticipantRepository;
    private final UserDeletedMessageOnlyMeRepository userDeletedMessageOnlyMeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final HiddenChatAccess hiddenChatAccess;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MessageListResponseDto getMessages(Long conversationId, Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(
                        () -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        UserParticipant participant = userParticipantRepository
                .findByUserIdAndConversationId(currentUserId, conversationId)
                .filter(member -> member.getLeftAt() == null)
                .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED,
                        "Bạn không tham gia cuộc hội thoại này"));
        hiddenChatAccess.requireIfHidden(participant);

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;

        List<Message> messages = messageRepository.findMessagesCursor(conversationId, currentUserId, cursor, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (messages.size() > pageSize) {
            hasNext = true;
            messages = messages.subList(0, pageSize);
            nextCursor = messages.getLast().getId();
        }

        // Batch fetch sender profiles to eliminate N+1
        Set<Long> senderUserIds = new HashSet<>();
        Set<Long> replyIds = new HashSet<>();
        for (Message m : messages) {
            if (m.getSender() != null) {
                senderUserIds.add(m.getSender().getId());
            }
            Message reply = m.getReplyToMessage();
            if (reply != null && Objects.equals(reply.getConversation().getId(), conversationId)) {
                replyIds.add(reply.getId());
                senderUserIds.add(reply.getSender().getId());
            }
        }

        Set<Long> deletedReplyIds = replyIds.isEmpty() ? Set.of()
                : userDeletedMessageOnlyMeRepository.findDeletedMessageIds(currentUserId, replyIds);
        Map<Long, UserProfile> profileMap = senderUserIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(senderUserIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<MessageDto> messageDtos = messages.stream().map(m -> {
            User sender = m.getSender();
            UserProfile senderProfile = profileMap.get(sender.getId());

            boolean revoked = m.getDeletedAt() != null;
            String content = revoked ? "Tin nhắn đã bị thu hồi" : m.getContent();
            List<String> mediaUrls = (revoked || m.getMediaUrls() == null) ? null
                    : m.getMediaUrls().stream().map(MediaItem::url).collect(Collectors.toList());

            ReplyMessagePreviewDto replyPreview = null;
            Message reply = m.getReplyToMessage();
            if (reply != null && Objects.equals(reply.getConversation().getId(), conversationId)) {
                if (deletedReplyIds.contains(reply.getId())) {
                    replyPreview = ReplyMessagePreviewDto.unavailable(reply.getId());
                } else {
                    User replySender = reply.getSender();
                    UserProfile replyProfile = profileMap.get(replySender.getId());
                    String replySenderName = replyProfile != null && replyProfile.getFullName() != null
                            ? replyProfile.getFullName() : replySender.getEmail();
                    replyPreview = ReplyMessagePreviewDto.fromMessage(reply, replySenderName);
                }
            }

            return MessageDto.builder()
                    .messageId(m.getId())
                    .senderId(sender.getId())
                    .senderName(senderProfile != null ? senderProfile.getFullName() : sender.getEmail())
                    .avatarUrl(senderProfile != null ? senderProfile.getAvatarUrl() : null)
                    .frameUrl(senderProfile != null ? senderProfile.getAvatarFrameUrl() : null)
                    .content(content)
                    .messageType(m.getMessageType())
                    .mediaUrls(mediaUrls)
                    .replyToMessage(replyPreview)
                    .createdAt(m.getCreatedAt())
                    .isEdited(m.getEditedAt() != null)
                    .revoked(revoked)
                    .build();
        }).collect(Collectors.toList());

        // Loading older history must not move the read receipt backwards.
        if (cursor == Long.MAX_VALUE && !messages.isEmpty()) {
            Message newestMessage = messages.getFirst();
            Message lastRead = participant.getLastReadMessage();
            if (lastRead == null || newestMessage.getId() > lastRead.getId()) {
                participant.setLastReadMessage(newestMessage);
                participant.setLastMessageReadAt(Instant.now());
                userParticipantRepository.save(participant);
            }
        }

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextCursor)
                .hasNext(hasNext)
                .build();

        return MessageListResponseDto.builder()
                .conversationId(conversationId)
                .conversationType(conversation.getType())
                .groupName(conversation.getType() == ConversationType.GROUP ? conversation.getName() : null)
                .groupAvatarUrl(conversation.getType() == ConversationType.GROUP ? conversation.getAvatarUrl() : null)
                .hiddenAt(participant.getHiddenAt())
                .messages(messageDtos)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public DeleteMessageResponseDto deleteMessage(Long messageId, DeleteMessageRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ResponseCode.MESSAGE_NOT_FOUND, "Không tìm thấy tin nhắn"));

        Long conversationId = message.getConversation().getId();
        UserParticipant participant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .filter(member -> member.getLeftAt() == null)
                .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED,
                        "Bạn không tham gia cuộc hội thoại chứa tin nhắn này"));
        hiddenChatAccess.requireIfHidden(participant);

        if (request.getScope() == MessageDeleteScope.ME) {
            boolean alreadyDeleted = userDeletedMessageOnlyMeRepository.existsByUserIdAndMessageId(currentUserId,
                    messageId);
            if (!alreadyDeleted) {
                User currentUser = userRepository.findById(currentUserId)
                        .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

                UserDeletedMessageOnlyMe deletedRecord = UserDeletedMessageOnlyMe.builder()
                        .id(new UserDeletedMessageOnlyMeId(currentUserId, messageId))
                        .user(currentUser)
                        .deletedMessageOnlyMe(message)
                        .deletedAt(Instant.now())
                        .build();

                userDeletedMessageOnlyMeRepository.save(deletedRecord);
            }

            return DeleteMessageResponseDto.builder()
                    .messageId(messageId)
                    .scope(MessageDeleteScope.ME)
                    .note("Tin nhắn đã được ẩn ở phía bạn")
                    .build();
        } else {
            // scope == EVERYONE
            if (!Objects.equals(message.getSender().getId(), currentUserId)) {
                throw new AppException(ResponseCode.NOT_MESSAGE_OWNER,
                        "Bạn chỉ có thể thu hồi tin nhắn do chính bạn gửi");
            }

            if (message.getDeletedAt() == null) {
                message.setDeletedAt(Instant.now());
                messageRepository.save(message);
                eventPublisher.publishEvent(new WsMessageRevokedDto(conversationId, messageId));
            }

            return DeleteMessageResponseDto.builder()
                    .messageId(messageId)
                    .scope(MessageDeleteScope.EVERYONE)
                    .note("Tin nhắn đã được thu hồi với tất cả mọi người")
                    .build();
        }
    }
}
