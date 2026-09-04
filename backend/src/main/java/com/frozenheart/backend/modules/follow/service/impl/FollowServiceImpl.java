package com.frozenheart.backend.modules.follow.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import com.frozenheart.backend.core.entity.socialinteraction.UserFollow;
import com.frozenheart.backend.core.entity.socialinteraction.UserFollowId;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.follow.dto.*;
import com.frozenheart.backend.modules.follow.service.FollowService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final BlockRepository blockRepository;
    private final CounterMetricsService counterMetricsService;

    @Override
    @Transactional
    public FollowResponseDto followUser(Long targetUserId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new AppException(ResponseCode.CANNOT_INTERACT_WITH_SELF, "Không thể tự theo dõi chính mình");
        }

        if (blockRepository.isBlockedBetween(currentUserId, targetUserId)) {
            throw new AppException(ResponseCode.USER_IS_BLOCKED, "Không thể tương tác do mối quan hệ bị chặn");
        }

        UserProfile targetProfile = userProfileRepository.findByUserIdWithUser(targetUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        Optional<UserFollow> existing = followRepository.findByFollowerIdAndFollowedUserId(currentUserId, targetUserId);

        LocalDateTime followedAt;
        int newFollowersCount;

        if (existing.isPresent()) {
            followedAt = existing.get().getCreatedAt();
            newFollowersCount = targetProfile.getFollowersCount();
        } else {
            followedAt = LocalDateTime.now();
            UserFollowId followId = new UserFollowId(currentUserId, targetUserId);
            UserFollow newFollow = UserFollow.builder()
                    .id(followId)
                    .follower(currentUser)
                    .followedUser(targetProfile.getUser())
                    .createdAt(followedAt)
                    .build();

            followRepository.save(newFollow);
            counterMetricsService.updateFollowerRelationCounts(currentUserId, targetUserId, 1);
            newFollowersCount = targetProfile.getFollowersCount() + 1;
        }

        return FollowResponseDto.builder()
                .targetUserId(targetUserId)
                .isFollowing(true)
                .newFollowersCount(newFollowersCount)
                .followedAt(followedAt)
                .build();
    }

    @Override
    @Transactional
    public UnfollowResponseDto unfollowUser(Long targetUserId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        UserProfile targetProfile = userProfileRepository.findByUserIdWithUser(targetUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        UserFollow follow = followRepository.findByFollowerIdAndFollowedUserId(currentUserId, targetUserId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Chưa theo dõi người dùng này"));

        followRepository.delete(follow);
        counterMetricsService.updateFollowerRelationCounts(currentUserId, targetUserId, -1);

        int newFollowersCount = Math.max(0, targetProfile.getFollowersCount() - 1);

        return UnfollowResponseDto.builder()
                .targetUserId(targetUserId)
                .isFollowing(false)
                .newFollowersCount(newFollowersCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowingListResponseDto getFollowing(Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        LocalDateTime cursor = (after != null && after > 0)
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(after), ZoneId.systemDefault())
                : LocalDateTime.now().plusYears(100);

        List<UserFollow> followList = followRepository.findFollowingCursor(currentUserId, cursor, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (followList.size() > pageSize) {
            hasNext = true;
            followList = followList.subList(0, pageSize);
            LocalDateTime lastCreatedAt = followList.getLast().getCreatedAt();
            nextCursor = lastCreatedAt != null ? lastCreatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
        }

        List<Long> followedUserIds = followList.stream()
                .map(f -> f.getFollowedUser().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserProfile> profileMap = followedUserIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(followedUserIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // Batch queries to avoid N+1
        Set<Long> followersWhoFollowMe = followedUserIds.isEmpty() ? Set.of()
                : followRepository.findFollowerIdsFollowingUser(currentUserId, followedUserIds);

        Set<Long> acceptedFriends = followedUserIds.isEmpty() ? Set.of()
                : friendshipRepository.findAcceptedFriendUserIds(currentUserId, followedUserIds);

        List<FollowingUserDto> items = followList.stream()
                .map(f -> {
                    User followedUser = f.getFollowedUser();
                    UserProfile profile = profileMap.get(followedUser.getId());
                    boolean isFollowingMe = followersWhoFollowMe.contains(followedUser.getId());
                    boolean isFriend = acceptedFriends.contains(followedUser.getId());

                    return FollowingUserDto.builder()
                            .userId(followedUser.getId())
                            .fullName(profile != null ? profile.getFullName() : followedUser.getEmail())
                            .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                            .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                            .role(followedUser.getRole())
                            .faculty(profile != null ? profile.getFaculty() : null)
                            .code(profile != null ? profile.getStudentLecturerCode() : null)
                            .followedAt(f.getCreatedAt())
                            .isFollowingMe(isFollowingMe)
                            .isFriend(isFriend)
                            .build();
                })
                .collect(Collectors.toList());

        int totalFollowing = followRepository.countByFollowerId(currentUserId);

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextCursor)
                .hasNext(hasNext)
                .build();

        return FollowingListResponseDto.builder()
                .items(items)
                .pagination(pagination)
                .totalFollowing(totalFollowing)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowerListResponseDto getFollowers(Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        LocalDateTime cursor = (after != null && after > 0)
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(after), ZoneId.systemDefault())
                : LocalDateTime.now().plusYears(100);

        List<UserFollow> followList = followRepository.findFollowersCursor(currentUserId, cursor, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (followList.size() > pageSize) {
            hasNext = true;
            followList = followList.subList(0, pageSize);
            LocalDateTime lastCreatedAt = followList.getLast().getCreatedAt();
            nextCursor = lastCreatedAt != null ? lastCreatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
        }

        List<Long> followerUserIds = followList.stream()
                .map(f -> f.getFollower().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserProfile> profileMap = followerUserIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(followerUserIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // Batch queries to avoid N+1
        Set<Long> followingBack = followerUserIds.isEmpty() ? Set.of()
                : followRepository.findFollowedIdsFollowedByUser(currentUserId, followerUserIds);

        Set<Long> acceptedFriends = followerUserIds.isEmpty() ? Set.of()
                : friendshipRepository.findAcceptedFriendUserIds(currentUserId, followerUserIds);

        List<FollowerUserDto> items = followList.stream()
                .map(f -> {
                    User followerUser = f.getFollower();
                    UserProfile profile = profileMap.get(followerUser.getId());
                    boolean isFollowingBack = followingBack.contains(followerUser.getId());
                    boolean isFriend = acceptedFriends.contains(followerUser.getId());

                    return FollowerUserDto.builder()
                            .userId(followerUser.getId())
                            .fullName(profile != null ? profile.getFullName() : followerUser.getEmail())
                            .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                            .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                            .role(followerUser.getRole())
                            .faculty(profile != null ? profile.getFaculty() : null)
                            .code(profile != null ? profile.getStudentLecturerCode() : null)
                            .followedMeAt(f.getCreatedAt())
                            .isFollowingBack(isFollowingBack)
                            .isFriend(isFriend)
                            .build();
                })
                .collect(Collectors.toList());

        int totalFollowers = followRepository.countByFollowedUserId(currentUserId);

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextCursor)
                .hasNext(hasNext)
                .build();

        return FollowerListResponseDto.builder()
                .items(items)
                .pagination(pagination)
                .totalFollowers(totalFollowers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RelationshipStatsResponseDto getRelationshipStats(Long userId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        UserProfile profile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        int followersCount = profile.getFollowersCount();
        int followingCount = profile.getFollowingCount();
        int friendsCount = profile.getFriendsCount();

        boolean isSelf = Objects.equals(currentUserId, userId);

        boolean isFollowing = false;
        boolean isFollowedBy = false;
        boolean isFriend = false;
        FriendshipStatus friendRequestStatus = FriendshipStatus.NONE;
        boolean isBlocked = false;

        if (!isSelf) {
            isFollowing = followRepository.existsByFollowerIdAndFollowedUserId(currentUserId, userId);
            isFollowedBy = followRepository.existsByFollowerIdAndFollowedUserId(userId, currentUserId);

            Optional<Friendship> friendshipOpt = friendshipRepository.findFriendshipsBetween(currentUserId, userId);
            isFriend = friendshipOpt.map(f -> f.getStatus() == FriendshipStatus.ACCEPTED).orElse(false);
            friendRequestStatus = friendshipOpt.map(Friendship::getStatus).orElse(FriendshipStatus.NONE);

            isBlocked = blockRepository.isBlockedBetween(currentUserId, userId);
        }

        return RelationshipStatsResponseDto.builder()
                .userId(userId)
                .counts(RelationshipStatsResponseDto.Counts.builder()
                        .followersCount(followersCount)
                        .followingCount(followingCount)
                        .friendsCount(friendsCount)
                        .build())
                .relationshipWithMe(RelationshipStatsResponseDto.RelationshipWithMe.builder()
                        .isFollowing(isFollowing)
                        .isFollowedBy(isFollowedBy)
                        .isFriend(isFriend)
                        .friendRequestStatus(friendRequestStatus)
                        .isBlocked(isBlocked)
                        .build())
                .build();
    }
}
