package com.frozenheart.backend.modules.friendship.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;
import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipId;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import com.frozenheart.backend.modules.follow.service.FollowService;
import com.frozenheart.backend.modules.friendship.dto.*;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.friendship.service.FriendshipService;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

        private final FriendshipRepository friendshipRepository;
        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final BlockRepository blockRepository;
        private final CounterMetricsService counterMetricsService;
        private final FollowService followService;
        private final BadgeService badgeService;

        @Override
        @Transactional
        public FriendshipResponseDto sendFriendRequest(SendFriendRequestDto request) {
                Long requesterId = JwtPayload.getCurrentUserPayload().getUserId();
                Long addresseeId = request.getAddresseeId();

                if (Objects.equals(requesterId, addresseeId)) {
                        throw new AppException(ResponseCode.CANNOT_INTERACT_WITH_SELF,
                                        "Không thể gửi lời mời kết bạn cho chính mình");
                }

                if (blockRepository.isBlockedBetween(requesterId, addresseeId)) {
                        throw new AppException(ResponseCode.USER_IS_BLOCKED,
                                        "Không thể tương tác do mối quan hệ bị chặn");
                }

                User requester = userRepository.findById(requesterId)
                                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
                User addressee = userRepository.findById(addresseeId)
                                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

                if (requester.getRole() != addressee.getRole()) {
                        throw new AppException(ResponseCode.ACTION_NOT_ALLOWED,
                                        "Chỉ người dùng đồng cấp (cùng vai trò Sinh viên - Sinh viên hoặc Giảng viên - Giảng viên) mới có thể kết bạn với nhau");
                }

                Optional<Friendship> existingOpt = friendshipRepository.findFriendshipsBetween(requesterId,
                                addresseeId);

                if (existingOpt.isPresent()) {
                        Friendship existing = existingOpt.get();
                        if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                                throw new AppException(ResponseCode.ALREADY_FRIENDS, "Hai người đã là bạn bè của nhau");
                        }
                        if (existing.getStatus() == FriendshipStatus.PENDING) {
                                if (Objects.equals(existing.getSender().getId(), requesterId)) {
                                        throw new AppException(ResponseCode.FRIEND_REQUEST_PENDING,
                                                        "Bạn đã gửi lời mời kết bạn cho người này rồi");
                                } else {
                                        // Người kia đã gửi lời mời cho mình trước đó -> Tự động chấp nhận
                                        existing.setStatus(FriendshipStatus.ACCEPTED);
                                        existing.setAcceptedAt(Instant.now());
                                        Friendship saved = friendshipRepository.save(existing);

                                        counterMetricsService.incrementFriendsCount(requesterId, 1);
                                        counterMetricsService.incrementFriendsCount(addresseeId, 1);

                                        // Tự động follow addressee (nếu chưa follow)
                                        followService.followUser(addresseeId);

                                        return mapToResponseDto(saved);
                                }
                        }
                }

                Instant now = Instant.now();
                FriendshipId friendshipId = new FriendshipId(requesterId, addresseeId);
                Friendship newFriendship = Friendship.builder()
                                .id(friendshipId)
                                .sender(requester)
                                .receiver(addressee)
                                .status(FriendshipStatus.PENDING)
                                .createdAt(now)
                                .build();

                Friendship saved = friendshipRepository.save(newFriendship);

                // Tự động follow addressee khi gửi lời mời kết bạn (nếu chưa follow)
                followService.followUser(addresseeId);

                return mapToResponseDto(saved);
        }

        @Override
        @Transactional
        public FriendshipResponseDto acceptFriendRequest(AcceptFriendRequestDto request) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                Long requesterId = request.getRequesterId();

                if (blockRepository.isBlockedBetween(currentUserId, requesterId)) {
                        throw new AppException(ResponseCode.USER_IS_BLOCKED,
                                        "Không thể tương tác do mối quan hệ bị chặn");
                }

                Friendship friendship = friendshipRepository.findFriendshipsBetween(requesterId, currentUserId)
                                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND,
                                                "Không tìm thấy lời mời kết bạn"));

                if (friendship.getStatus() != FriendshipStatus.PENDING
                                || !Objects.equals(friendship.getReceiver().getId(), currentUserId)) {
                        throw new AppException(ResponseCode.ACTION_NOT_ALLOWED,
                                        "Bạn không có quyền chấp nhận lời mời kết bạn này");
                }

                if (friendship.getSender().getRole() != friendship.getReceiver().getRole()) {
                        throw new AppException(ResponseCode.ACTION_NOT_ALLOWED,
                                        "Chỉ người dùng đồng cấp (cùng vai trò) mới có thể chấp nhận kết bạn");
                }

                friendship.setStatus(FriendshipStatus.ACCEPTED);
                friendship.setAcceptedAt(Instant.now());

                Friendship saved = friendshipRepository.save(friendship);

                counterMetricsService.incrementFriendsCount(currentUserId, 1);
                counterMetricsService.incrementFriendsCount(requesterId, 1);

                badgeService.checkAndGrantBadges(friendship.getReceiver(), BadgeTriggerEvent.ACCEPTED_FRIENDS, null);
                badgeService.checkAndGrantBadges(friendship.getSender(), BadgeTriggerEvent.ACCEPTED_FRIENDS, null);

                // Tự động follow requester khi chấp nhận lời mời kết bạn (nếu chưa follow)
                followService.followUser(requesterId);

                return mapToResponseDto(saved);
        }

        @Override
        @Transactional
        public void declineFriendRequest(DeclineFriendRequestDto request) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                Long requesterId = request.getRequesterId();

                Friendship friendship = friendshipRepository.findFriendshipsBetween(requesterId, currentUserId)
                                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND,
                                                "Không tìm thấy lời mời kết bạn"));

                if (!Objects.equals(friendship.getReceiver().getId(), currentUserId)) {
                        throw new AppException(ResponseCode.ACTION_NOT_ALLOWED,
                                        "Bạn không có quyền từ chối lời mời kết bạn này");
                }

                // Xóa hẳn bản ghi PENDING khỏi DB (không giảm friendsCount vì chưa bao giờ là
                // ACCEPTED)
                friendshipRepository.delete(friendship);
        }

        @Override
        @Transactional
        public void unfriend(Long friendId) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                Friendship friendship = friendshipRepository.findFriendshipsBetween(currentUserId, friendId)
                                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND,
                                                "Hai người chưa là bạn bè của nhau"));

                if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
                        throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Hai người chưa là bạn bè của nhau");
                }

                // Giảm friendsCount cho cả 2 người vì đang là ACCEPTED
                counterMetricsService.incrementFriendsCount(currentUserId, -1);
                counterMetricsService.incrementFriendsCount(friendId, -1);

                friendshipRepository.delete(friendship);
        }

        @Override
        @Transactional(readOnly = true)
        public FriendListResponseDto getMyFriends(Long after, Integer limit) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                Instant cursor = (after != null && after > 0)
                                ? Instant.ofEpochMilli(after)
                                : Instant.now().plus(36500, ChronoUnit.DAYS);

                List<Friendship> friendsList = friendshipRepository.findAcceptedFriendsCursor(currentUserId, cursor,
                                pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (friendsList.size() > pageSize) {
                        hasNext = true;
                        friendsList = friendsList.subList(0, pageSize);
                        Instant lastAcceptedAt = friendsList.getLast().getAcceptedAt();
                        nextCursor = lastAcceptedAt != null
                                        ? lastAcceptedAt.toEpochMilli()
                                        : null;
                }

                List<Long> friendUserIds = friendsList.stream()
                                .map(f -> Objects.equals(f.getSender().getId(), currentUserId) ? f.getReceiver().getId()
                                                : f.getSender().getId())
                                .distinct()
                                .collect(Collectors.toList());

                Map<Long, UserProfile> profileMap = friendUserIds.isEmpty() ? Map.of()
                                : userProfileRepository.findAllById(friendUserIds).stream()
                                                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

                List<FriendDto> items = friendsList.stream()
                                .map(f -> {
                                        Long friendUserId = Objects.equals(f.getSender().getId(), currentUserId)
                                                        ? f.getReceiver().getId()
                                                        : f.getSender().getId();
                                        UserProfile profile = profileMap.get(friendUserId);
                                        return FriendDto.builder()
                                                        .userId(friendUserId)
                                                        .fullName(profile != null ? profile.getFullName() : "")
                                                        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                                                        .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                                                        .friendsSince(f.getAcceptedAt())
                                                        .mutualFriendsCount(0)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                int totalFriends = friendshipRepository.countTotalFriends(currentUserId);

                CursorPaginationDto pagination = CursorPaginationDto.builder()
                                .after(nextCursor)
                                .hasNext(hasNext)
                                .build();

                return FriendListResponseDto.builder()
                                .items(items)
                                .pagination(pagination)
                                .totalFriends(totalFriends)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public FriendRequestReceivedListResponseDto getReceivedFriendRequests(Long after, Integer limit) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                Instant cursor = (after != null && after > 0)
                                ? Instant.ofEpochMilli(after)
                                : Instant.now().plus(36500, ChronoUnit.DAYS);

                List<Friendship> requestsList = friendshipRepository.findReceivedRequests(currentUserId, cursor,
                                pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (requestsList.size() > pageSize) {
                        hasNext = true;
                        requestsList = requestsList.subList(0, pageSize);
                        Instant lastCreatedAt = requestsList.getLast().getCreatedAt();
                        nextCursor = lastCreatedAt != null
                                        ? lastCreatedAt.toEpochMilli()
                                        : null;
                }

                List<Long> requesterIds = requestsList.stream()
                                .map(f -> f.getSender().getId())
                                .distinct()
                                .collect(Collectors.toList());

                Map<Long, UserProfile> profileMap = requesterIds.isEmpty() ? Map.of()
                                : userProfileRepository.findAllById(requesterIds).stream()
                                                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

                List<FriendRequestReceivedDto> items = requestsList.stream()
                                .map(f -> {
                                        User requester = f.getSender();
                                        UserProfile profile = profileMap.get(requester.getId());
                                        AuthorDto authorDto = AuthorDto.builder()
                                                        .userId(requester.getId())
                                                        .fullName(profile != null ? profile.getFullName()
                                                                        : requester.getEmail())
                                                        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                                                        .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                                                        .build();

                                        return FriendRequestReceivedDto.builder()
                                                        .requester(authorDto)
                                                        .status(f.getStatus())
                                                        .createdAt(f.getCreatedAt())
                                                        .mutualFriendsCount(0)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                int totalPending = friendshipRepository.countByReceiverIdAndStatus(currentUserId,
                                FriendshipStatus.PENDING);

                CursorPaginationDto pagination = CursorPaginationDto.builder()
                                .after(nextCursor)
                                .hasNext(hasNext)
                                .build();

                return FriendRequestReceivedListResponseDto.builder()
                                .items(items)
                                .pagination(pagination)
                                .totalPending(totalPending)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public FriendRequestSentListResponseDto getSentFriendRequests(Long after, Integer limit) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                Instant cursor = (after != null && after > 0)
                                ? Instant.ofEpochMilli(after)
                                : Instant.now().plus(36500, ChronoUnit.DAYS);

                List<Friendship> requestsList = friendshipRepository.findSentRequests(currentUserId, cursor, pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (requestsList.size() > pageSize) {
                        hasNext = true;
                        requestsList = requestsList.subList(0, pageSize);
                        Instant lastCreatedAt = requestsList.getLast().getCreatedAt();
                        nextCursor = lastCreatedAt != null
                                        ? lastCreatedAt.toEpochMilli()
                                        : null;
                }

                List<Long> receiverIds = requestsList.stream()
                                .map(f -> f.getReceiver().getId())
                                .distinct()
                                .collect(Collectors.toList());

                Map<Long, UserProfile> profileMap = receiverIds.isEmpty() ? Map.of()
                                : userProfileRepository.findAllById(receiverIds).stream()
                                                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

                List<FriendRequestSentDto> items = requestsList.stream()
                                .map(f -> {
                                        User receiver = f.getReceiver();
                                        UserProfile profile = profileMap.get(receiver.getId());
                                        AuthorDto authorDto = AuthorDto.builder()
                                                        .userId(receiver.getId())
                                                        .fullName(profile != null ? profile.getFullName()
                                                                        : receiver.getEmail())
                                                        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                                                        .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                                                        .build();

                                        return FriendRequestSentDto.builder()
                                                        .receiver(authorDto)
                                                        .status(f.getStatus())
                                                        .createdAt(f.getCreatedAt())
                                                        .mutualFriendsCount(0)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                int totalSent = friendshipRepository.countBySenderIdAndStatus(currentUserId, FriendshipStatus.PENDING);

                CursorPaginationDto pagination = CursorPaginationDto.builder()
                                .after(nextCursor)
                                .hasNext(hasNext)
                                .build();

                return FriendRequestSentListResponseDto.builder()
                                .items(items)
                                .pagination(pagination)
                                .totalSent(totalSent)
                                .build();
        }

        private FriendshipResponseDto mapToResponseDto(Friendship friendship) {
                return FriendshipResponseDto.builder()
                                .requesterId(friendship.getSender().getId())
                                .addresseeId(friendship.getReceiver().getId())
                                .status(friendship.getStatus())
                                .createdAt(friendship.getCreatedAt())
                                .acceptedAt(friendship.getAcceptedAt())
                                .build();
        }
}
