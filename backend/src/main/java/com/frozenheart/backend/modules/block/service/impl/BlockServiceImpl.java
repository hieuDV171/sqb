package com.frozenheart.backend.modules.block.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.socialinteraction.*;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.block.dto.*;
import com.frozenheart.backend.modules.block.service.BlockService;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.FollowRepository;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
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
public class BlockServiceImpl implements BlockService {

        private final BlockRepository blockRepository;
        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final FriendshipRepository friendshipRepository;
        private final FollowRepository followRepository;
        private final CounterMetricsService counterMetricsService;

        @Override
        @Transactional
        public BlockResponseDto blockUser(Long targetUserId) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                if (Objects.equals(currentUserId, targetUserId)) {
                        throw new AppException(ResponseCode.CANNOT_INTERACT_WITH_SELF, "Không thể tự chặn chính mình");
                }

                if (blockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
                        throw new AppException(ResponseCode.ALREADY_BLOCKED, "Bạn đã chặn người dùng này rồi");
                }

                User blocker = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
                User blocked = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

                UserBlockId blockId = new UserBlockId(currentUserId, targetUserId);
                UserBlock userBlock = UserBlock.builder()
                                .id(blockId)
                                .blocker(blocker)
                                .blocked(blocked)
                                .createdAt(Instant.now())
                                .build();

                blockRepository.save(userBlock);

                // Hủy quan hệ bạn bè (Friendship) nếu có
                Optional<Friendship> friendshipOpt = friendshipRepository.findFriendshipsBetween(currentUserId,
                                targetUserId);
                if (friendshipOpt.isPresent()) {
                        Friendship friendship = friendshipOpt.get();
                        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                                counterMetricsService.incrementFriendsCount(currentUserId, -1);
                                counterMetricsService.incrementFriendsCount(targetUserId, -1);
                        }
                        friendshipRepository.delete(friendship);
                }

                // Hủy quan hệ follow (Chiều currentUserId -> targetUserId)
                Optional<UserFollow> followCurrentToTarget = followRepository
                                .findByFollowerIdAndFollowedUserId(currentUserId, targetUserId);
                if (followCurrentToTarget.isPresent()) {
                        followRepository.delete(followCurrentToTarget.get());
                        counterMetricsService.updateFollowerRelationCounts(currentUserId, targetUserId, -1);
                }

                // Hủy quan hệ follow (Chiều targetUserId -> currentUserId)
                Optional<UserFollow> followTargetToCurrent = followRepository
                                .findByFollowerIdAndFollowedUserId(targetUserId, currentUserId);
                if (followTargetToCurrent.isPresent()) {
                        followRepository.delete(followTargetToCurrent.get());
                        counterMetricsService.updateFollowerRelationCounts(targetUserId, currentUserId, -1);
                }

                return BlockResponseDto.builder()
                                .blockedUserId(targetUserId)
                                .isBlocked(true)
                                .build();
        }

        @Override
        @Transactional
        public UnblockResponseDto unblockUser(Long targetUserId) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                UserBlock block = blockRepository.findByBlockerIdAndBlockedId(currentUserId, targetUserId)
                                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND,
                                                "Chưa chặn người dùng này"));

                blockRepository.delete(block);

                return UnblockResponseDto.builder()
                                .unblockedUserId(targetUserId)
                                .isBlocked(false)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public BlockedListResponseDto getBlockedUsers(Long after, Integer limit) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                Instant cursor = (after != null && after > 0)
                                ? Instant.ofEpochMilli(after)
                                : Instant.now().plus(36500, ChronoUnit.DAYS);

                List<UserBlock> blockList = blockRepository.findBlockedUsersCursor(currentUserId, cursor, pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (blockList.size() > pageSize) {
                        hasNext = true;
                        blockList = blockList.subList(0, pageSize);
                        Instant lastCreatedAt = blockList.getLast().getCreatedAt();
                        nextCursor = lastCreatedAt != null
                                        ? lastCreatedAt.toEpochMilli()
                                        : null;
                }

                List<Long> blockedUserIds = blockList.stream()
                                .map(b -> b.getBlocked().getId())
                                .distinct()
                                .collect(Collectors.toList());

                Map<Long, UserProfile> profileMap = blockedUserIds.isEmpty() ? Map.of()
                                : userProfileRepository.findAllById(blockedUserIds).stream()
                                                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

                List<BlockedUserDto> items = blockList.stream()
                                .map(b -> {
                                        User blockedUser = b.getBlocked();
                                        UserProfile profile = profileMap.get(blockedUser.getId());
                                        return BlockedUserDto.builder()
                                                        .userId(blockedUser.getId())
                                                        .fullName(profile != null ? profile.getFullName()
                                                                        : blockedUser.getEmail())
                                                        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                                                        .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                                                        .blockedAt(b.getCreatedAt())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                int totalBlocked = blockRepository.countByBlockerId(currentUserId);

                CursorPaginationDto pagination = CursorPaginationDto.builder()
                                .after(nextCursor)
                                .hasNext(hasNext)
                                .build();

                return BlockedListResponseDto.builder()
                                .items(items)
                                .pagination(pagination)
                                .totalBlocked(totalBlocked)
                                .build();
        }
}
