package com.frozenheart.backend.modules.conversation.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.conversation.*;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.dto.*;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.ConversationService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserParticipantRepository userParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BlockRepository blockRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaService mediaService;

    @Override
    @Transactional(readOnly = true)
    public ConversationListResponseDto getConversations(Long after, Integer limit) {
        final boolean isHidden = false; // Tăng tính dễ đọc
        return fetchConversationsInternal(after, limit, isHidden);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationListResponseDto getHiddenConversations(Long after, Integer limit) {
        final boolean isHidden = true;
        return fetchConversationsInternal(after, limit, isHidden);
    }

    private ConversationListResponseDto fetchConversationsInternal(Long after, Integer limit, boolean isHidden) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        // [CẢNH BÁO] Zone khác nhau cho kết quả khác nhau.
        LocalDateTime cursor = (after != null && after > 0)
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(after), ZoneId.systemDefault())
                : LocalDateTime.now().plusYears(100);

        List<Conversation> conversations = conversationRepository.findUserConversationsCursor(
                currentUserId, isHidden, cursor, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (conversations.size() > pageSize) {
            hasNext = true;
            conversations = conversations.subList(0, pageSize);
            LocalDateTime lastMessageAt = conversations.getLast().getLastMessageAt();
            nextCursor = lastMessageAt != null
                    ? lastMessageAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : null;
        }

        if (conversations.isEmpty()) {
            return ConversationListResponseDto.builder()
                    .items(Collections.emptyList())
                    .pagination(CursorPaginationDto.builder().after(null).hasNext(false).build())
                    .totalConversations(0)
                    .build();
        }

        List<Long> convIds = conversations.stream().map(Conversation::getId).toList();

        // Batch fetch participants to avoid N+1
        List<UserParticipant> allParticipants = userParticipantRepository.findActiveParticipantsForConversations(convIds);
        Map<Long, List<UserParticipant>> participantsByConvId = allParticipants.stream()
                .collect(Collectors.groupingBy(p -> p.getConversation().getId()));

        Set<Long> userIds = allParticipants.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<ConversationDto> items = conversations.stream().map(c -> {
            List<UserParticipant> participants = participantsByConvId.getOrDefault(c.getId(), Collections.emptyList());

            UserParticipant myParticipant = participants.stream()
                    .filter(p -> Objects.equals(p.getUser().getId(), currentUserId))
                    .findFirst()
                    .orElse(null);

            String displayName = c.getName();
            String displayAvatarUrl = c.getAvatarUrl();
            int memberCount = participants.size();

            if (c.getType() == ConversationType.DIRECT) {
                UserParticipant otherParticipant = participants.stream()
                        .filter(p -> !Objects.equals(p.getUser().getId(), currentUserId))
                        .findFirst()
                        .orElse(null);

                if (otherParticipant != null) {
                    UserProfile otherProfile = profileMap.get(otherParticipant.getUser().getId());
                    displayName = (otherProfile != null && otherProfile.getFullName() != null)
                            ? otherProfile.getFullName()
                            : otherParticipant.getUser().getEmail();
                    displayAvatarUrl = (otherProfile != null) ? otherProfile.getAvatarUrl() : null;
                }
                memberCount = 2;
            }

            LastMessageDto lastMessageDto = null;
            if (c.getLastMessage() != null) {
                Message lastMsg = c.getLastMessage();
                String content = lastMsg.getDeletedAt() != null
                        ? "Tin nhắn đã bị thu hồi"
                        : lastMsg.getContent();

                lastMessageDto = LastMessageDto.builder()
                        .id(lastMsg.getId())
                        .content(content)
                        .messageType(lastMsg.getMessageType())
                        .createdAt(lastMsg.getCreatedAt())
                        .build();
            }

            Long lastReadMsgId = (myParticipant != null && myParticipant.getLastReadMessage() != null)
                    ? myParticipant.getLastReadMessage().getId()
                    : null;
            int unreadCount = messageRepository.countUnreadMessages(c.getId(), currentUserId, lastReadMsgId);

            return ConversationDto.builder()
                    .conversationId(c.getId())
                    .type(c.getType())
                    .displayName(displayName)
                    .displayAvatarUrl(displayAvatarUrl)
                    .memberCount(memberCount)
                    .lastMessage(lastMessageDto)
                    .unreadCount(unreadCount)
                    .hiddenAt(myParticipant != null ? myParticipant.getHiddenAt() : null)
                    .isMuted(myParticipant != null && myParticipant.isMuted())
                    .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreadtedAt())
                    .build();
        }).collect(Collectors.toList());

        int totalConversations = conversationRepository.countUserConversations(currentUserId, isHidden);

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextCursor)
                .hasNext(hasNext)
                .build();

        return ConversationListResponseDto.builder()
                .items(items)
                .pagination(pagination)
                .totalConversations(totalConversations)
                .build();
    }

    @Override
    @Transactional
    public ConversationDetailDto createDirectConversation(CreateDirectConversationDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        Long targetUserId = request.getTargetUserId();

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new AppException(ResponseCode.CANNOT_INTERACT_WITH_SELF, "Không thể tạo cuộc hội thoại với chính mình");
        }

        if (blockRepository.isBlockedBetween(currentUserId, targetUserId)) {
            throw new AppException(ResponseCode.USER_IS_BLOCKED, "Không thể tạo cuộc hội thoại do mối quan hệ bị chặn");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        Optional<Conversation> existingOpt = conversationRepository.findDirectConversationBetween(currentUserId, targetUserId);
        if (existingOpt.isPresent()) {
            Conversation existing = existingOpt.get();
            return mapToDetailDto(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .creater(currentUser)
                .creadtedAt(now)
                .updatedAt(now)
                .build();

        Conversation savedConv = conversationRepository.save(conversation);

        UserParticipant p1 = UserParticipant.builder()
                .id(new UserParticipantId(currentUserId, savedConv.getId()))
                .user(currentUser)
                .conversation(savedConv)
                .role(ConversationRole.MEMBER)
                .joinedAt(now)
                .build();

        UserParticipant p2 = UserParticipant.builder()
                .id(new UserParticipantId(targetUserId, savedConv.getId()))
                .user(targetUser)
                .conversation(savedConv)
                .role(ConversationRole.MEMBER)
                .joinedAt(now)
                .build();

        userParticipantRepository.saveAll(List.of(p1, p2));

        return mapToDetailDto(savedConv);
    }

    @Override
    @Transactional
    public ConversationDetailDto createGroupConversation(CreateGroupConversationDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        List<Long> memberIds = request.getMemberIds().stream()
                .filter(id -> !Objects.equals(id, currentUserId))
                .distinct()
                .toList();

        if (memberIds.size() < 2 || memberIds.size() > 199) {
            throw new AppException(ResponseCode.CONSTRAINTS_UNSATISFIED, "Nhóm phải có từ 3 đến 200 người (bao gồm người tạo và từ 2 đến 199 thành viên khác)");
        }

        List<User> targetMembers = userRepository.findAllById(memberIds);
        if (targetMembers.size() != memberIds.size()) {
            throw new AppException(ResponseCode.USER_NOT_FOUND, "Có thành viên không tồn tại trong hệ thống");
        }

        Set<Long> blockedUserIds = blockRepository.findBlockedUserIdsAmong(currentUserId, memberIds);
        if (!blockedUserIds.isEmpty()) {
            throw new AppException(ResponseCode.USER_IS_BLOCKED, "Không thể tạo nhóm với người dùng bị chặn");
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            mediaService.confirmMediaPermanent(List.of(request.getAvatarUrl()));
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .creater(currentUser)
                .creadtedAt(now)
                .updatedAt(now)
                .build();

        Conversation savedConv = conversationRepository.save(conversation);

        List<UserParticipant> participants = new ArrayList<>();
        participants.add(UserParticipant.builder()
                .id(new UserParticipantId(currentUserId, savedConv.getId()))
                .user(currentUser)
                .conversation(savedConv)
                .role(ConversationRole.ADMIN)
                .joinedAt(now)
                .build());

        for (User member : targetMembers) {
            participants.add(UserParticipant.builder()
                    .id(new UserParticipantId(member.getId(), savedConv.getId()))
                    .user(member)
                    .conversation(savedConv)
                    .role(ConversationRole.MEMBER)
                    .joinedAt(now)
                    .build());
        }

        userParticipantRepository.saveAll(participants);

        return mapToDetailDto(savedConv);
    }

    @Override
    @Transactional
    public void hideConversation(Long conversationId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        UserParticipant participant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND, "Cuộc hội thoại không tồn tại hoặc bạn không tham gia cuộc hội thoại này"));

        participant.setMuted(true);
        participant.setHiddenAt(LocalDateTime.now());
        userParticipantRepository.save(participant);
    }

    @Override
    @Transactional
    public void unhideConversation(Long conversationId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        UserParticipant participant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND, "Cuộc hội thoại không tồn tại hoặc bạn không tham gia cuộc hội thoại này"));

        participant.setMuted(false);
        participant.setHiddenAt(null);
        userParticipantRepository.save(participant);
    }

    @Override
    @Transactional
    public void setHiddenChatPin(SetHiddenChatPinDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        UserProfile profile = userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (profile.getHiddenChatPin() != null && !profile.getHiddenChatPin().isBlank()) {
            if (request.getOldPin() == null || !passwordEncoder.matches(request.getOldPin(), profile.getHiddenChatPin())) {
                throw new AppException(ResponseCode.INVALID_PIN, "Mã PIN cũ không chính xác");
            }
        }

        profile.setHiddenChatPin(passwordEncoder.encode(request.getPin()));
        userProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public void unlockHiddenChat(UnlockHiddenChatDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        UserProfile profile = userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (profile.getHiddenChatPin() == null || profile.getHiddenChatPin().isBlank()) {
            throw new AppException(ResponseCode.PIN_NOT_SET, "Chưa thiết lập mã PIN cho kho ẩn trò chuyện");
        }

        if (!passwordEncoder.matches(request.getPin(), profile.getHiddenChatPin())) {
            throw new AppException(ResponseCode.INVALID_PIN, "Mã PIN không chính xác");
        }
    }

    // --- Group Management (5.5.3) ---

    @Override
    @Transactional
    public ConversationDetailDto updateGroupInfo(Long conversationId, UpdateGroupInfoDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Chỉ có thể cập nhật thông tin cho nhóm chat");
        }

        UserParticipant participant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn không tham gia nhóm chat này"));

        if (participant.getLeftAt() != null || participant.getRole() != ConversationRole.ADMIN) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Chỉ quản trị viên mới có quyền cập nhật thông tin nhóm");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            conversation.setName(request.getName());
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            mediaService.confirmMediaPermanent(List.of(request.getAvatarUrl()));
            conversation.setAvatarUrl(request.getAvatarUrl());
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conversation);

        return mapToDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationMemberDto> getGroupMembers(Long conversationId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        boolean isParticipant = userParticipantRepository.existsByUserIdAndConversationIdAndLeftAtIsNull(currentUserId, conversationId);
        if (!isParticipant) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn không tham gia cuộc hội thoại này");
        }

        List<UserParticipant> participants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversationId);
        Set<Long> userIds = participants.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        return participants.stream().map(p -> {
            UserProfile profile = profileMap.get(p.getUser().getId());
            return ConversationMemberDto.builder()
                    .userId(p.getUser().getId())
                    .fullName(profile != null ? profile.getFullName() : p.getUser().getEmail())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .role(p.getRole())
                    .createdAt(p.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddGroupMembersResponseDto addGroupMembers(Long conversationId, AddGroupMembersRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Chỉ có thể thêm thành viên vào nhóm chat");
        }

        boolean isParticipant = userParticipantRepository.existsByUserIdAndConversationIdAndLeftAtIsNull(currentUserId, conversationId);
        if (!isParticipant) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn không tham gia nhóm chat này");
        }

        List<UserParticipant> currentActiveParticipants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversationId);
        Set<Long> currentMemberIds = currentActiveParticipants.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());

        List<Long> targetUserIds = request.getUserIds().stream()
                .filter(id -> !Objects.equals(id, currentUserId))
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(targetUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(targetUserIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Set<Long> blockedIds = blockRepository.findBlockedUserIdsAmong(currentUserId, targetUserIds);

        List<UserParticipant> existingParticipants = userParticipantRepository
                .findByConversationIdAndUserIdIn(conversationId, targetUserIds);
        Map<Long, UserParticipant> existingMap = existingParticipants.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        List<ConversationMemberDto> addedMembers = new ArrayList<>();
        List<FailedMemberDto> failedMembers = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        int maxCapacity = 200;

        for (Long targetUserId : targetUserIds) {
            User targetUser = userMap.get(targetUserId);
            UserProfile targetProfile = profileMap.get(targetUserId);

            String fullName = targetProfile != null ? targetProfile.getFullName() : (targetUser != null ? targetUser.getEmail() : "");
            String avatarUrl = targetProfile != null ? targetProfile.getAvatarUrl() : null;
            String frameUrl = targetProfile != null ? targetProfile.getAvatarFrameUrl() : null;

            if (targetUser == null) {
                failedMembers.add(new FailedMemberDto(targetUserId, fullName, avatarUrl, frameUrl, "Người dùng không tồn tại"));
                continue;
            }

            if (currentMemberIds.contains(targetUserId)) {
                failedMembers.add(new FailedMemberDto(targetUserId, fullName, avatarUrl, frameUrl, "Đã là thành viên của nhóm"));
                continue;
            }

            if (blockedIds.contains(targetUserId)) {
                failedMembers.add(new FailedMemberDto(targetUserId, fullName, avatarUrl, frameUrl, "Không thể thêm người dùng bị chặn"));
                continue;
            }

            if (currentMemberIds.size() + addedMembers.size() >= maxCapacity) {
                failedMembers.add(new FailedMemberDto(targetUserId, fullName, avatarUrl, frameUrl, "Nhóm đã đạt số lượng tối đa 200 thành viên"));
                continue;
            }

            UserParticipant participant = existingMap.get(targetUserId);
            if (participant != null) {
                participant.setLeftAt(null);
                participant.setJoinedAt(now);
                participant.setRole(ConversationRole.MEMBER);
            } else {
                participant = UserParticipant.builder()
                        .id(new UserParticipantId(targetUserId, conversationId))
                        .user(targetUser)
                        .conversation(conversation)
                        .role(ConversationRole.MEMBER)
                        .joinedAt(now)
                        .build();
            }

            userParticipantRepository.save(participant);

            addedMembers.add(ConversationMemberDto.builder()
                    .userId(targetUserId)
                    .fullName(fullName)
                    .avatarUrl(avatarUrl)
                    .frameUrl(frameUrl)
                    .role(ConversationRole.MEMBER)
                    .createdAt(now)
                    .build());
        }

        return AddGroupMembersResponseDto.builder()
                .addedMembers(addedMembers)
                .failedMembers(failedMembers)
                .build();
    }

    @Override
    @Transactional
    public void removeGroupMember(Long conversationId, Long targetUserId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Để rời nhóm vui lòng sử dụng chức năng rời nhóm");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        UserParticipant callerParticipant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn không tham gia nhóm chat này"));

        if (callerParticipant.getLeftAt() != null || callerParticipant.getRole() != ConversationRole.ADMIN) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Chỉ quản trị viên mới có quyền xóa thành viên");
        }

        UserParticipant targetParticipant = userParticipantRepository.findByUserIdAndConversationId(targetUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong nhóm"));

        if (targetParticipant.getLeftAt() != null) {
            throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Thành viên đã rời khỏi nhóm");
        }

        if (targetParticipant.getRole() == ConversationRole.ADMIN) {
            if (!Objects.equals(conversation.getCreater().getId(), currentUserId)) {
                throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Chỉ người tạo nhóm mới có quyền xóa quản trị viên khác");
            }
        }

        targetParticipant.setLeftAt(LocalDateTime.now());
        userParticipantRepository.save(targetParticipant);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long conversationId, Long targetUserId, UpdateMemberRoleRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Không thể tự thay đổi vai trò của chính mình");
        }

        UserParticipant callerParticipant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn không tham gia nhóm chat này"));

        if (callerParticipant.getLeftAt() != null || callerParticipant.getRole() != ConversationRole.ADMIN) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Chỉ quản trị viên mới có quyền thay đổi vai trò thành viên");
        }

        UserParticipant targetParticipant = userParticipantRepository.findByUserIdAndConversationId(targetUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong nhóm"));

        if (targetParticipant.getLeftAt() != null) {
            throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Thành viên đã rời khỏi nhóm");
        }

        if (request.getRole() == ConversationRole.MEMBER && targetParticipant.getRole() == ConversationRole.ADMIN) {
            List<UserParticipant> activeParticipants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversationId);
            long adminCount = activeParticipants.stream().filter(p -> p.getRole() == ConversationRole.ADMIN).count();
            if (adminCount <= 1) {
                throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Nhóm phải có ít nhất một quản trị viên");
            }
        }

        targetParticipant.setRole(request.getRole());
        userParticipantRepository.save(targetParticipant);
    }

    @Override
    @Transactional
    public LeaveGroupResponseDto leaveGroup(Long conversationId, LeaveGroupRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        UserParticipant callerParticipant = userParticipantRepository.findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn không tham gia nhóm chat này"));

        if (callerParticipant.getLeftAt() != null) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Bạn đã rời khỏi nhóm chat này rồi");
        }

        List<UserParticipant> otherActiveParticipants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversationId)
                .stream()
                .filter(p -> !Objects.equals(p.getUser().getId(), currentUserId))
                .toList();

        ConversationMemberDto newAdminDto = null;

        if (callerParticipant.getRole() == ConversationRole.ADMIN && !otherActiveParticipants.isEmpty()) {
            boolean hasOtherAdmin = otherActiveParticipants.stream().anyMatch(p -> p.getRole() == ConversationRole.ADMIN);

            if (!hasOtherAdmin) {
                UserParticipant nextAdmin;
                if (request != null && request.getNewAdminId() != null) {
                    nextAdmin = otherActiveParticipants.stream()
                            .filter(p -> Objects.equals(p.getUser().getId(), request.getNewAdminId()))
                            .findFirst()
                            .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Thành viên chỉ định làm quản trị viên mới không hợp lệ"));
                } else {
                    nextAdmin = otherActiveParticipants.stream()
                            .min(Comparator.comparing(UserParticipant::getJoinedAt))
                            .orElse(otherActiveParticipants.getFirst());
                }

                nextAdmin.setRole(ConversationRole.ADMIN);
                userParticipantRepository.save(nextAdmin);

                UserProfile nextAdminProfile = userProfileRepository.findByUserId(nextAdmin.getUser().getId()).orElse(null);
                newAdminDto = ConversationMemberDto.builder()
                        .userId(nextAdmin.getUser().getId())
                        .fullName(nextAdminProfile != null ? nextAdminProfile.getFullName() : nextAdmin.getUser().getEmail())
                        .avatarUrl(nextAdminProfile != null ? nextAdminProfile.getAvatarUrl() : null)
                        .frameUrl(nextAdminProfile != null ? nextAdminProfile.getAvatarFrameUrl() : null)
                        .role(ConversationRole.ADMIN)
                        .createdAt(nextAdmin.getJoinedAt())
                        .build();
            }
        }

        callerParticipant.setLeftAt(LocalDateTime.now());
        userParticipantRepository.save(callerParticipant);

        return LeaveGroupResponseDto.builder()
                .conversationId(conversationId)
                .newAdmin(newAdminDto)
                .build();
    }

    private ConversationDetailDto mapToDetailDto(Conversation conversation) {
        List<UserParticipant> participants = userParticipantRepository.findByConversationIdAndLeftAtIsNull(conversation.getId());
        Set<Long> userIds = participants.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<ConversationMemberDto> members = participants.stream().map(p -> {
            UserProfile profile = profileMap.get(p.getUser().getId());
            return ConversationMemberDto.builder()
                    .userId(p.getUser().getId())
                    .fullName(profile != null ? profile.getFullName() : p.getUser().getEmail())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .role(p.getRole())
                    .createdAt(p.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());

        return ConversationDetailDto.builder()
                .conversationId(conversation.getId())
                .type(conversation.getType())
                .name(conversation.getName())
                .avatarUrl(conversation.getAvatarUrl())
                .members(members)
                .createdAt(conversation.getCreadtedAt())
                .build();
    }
}
