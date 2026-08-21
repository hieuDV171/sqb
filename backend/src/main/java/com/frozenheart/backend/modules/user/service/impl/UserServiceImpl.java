package com.frozenheart.backend.modules.user.service.impl;

import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import com.frozenheart.backend.modules.user.dto.LockedFeature;
import com.frozenheart.backend.modules.user.dto.Relationship;
import com.frozenheart.backend.modules.user.repository.FollowRepository;
import com.frozenheart.backend.modules.user.repository.FriendshipRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.user.dto.ProfileResponse;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;
    private final FriendshipRepository friendshipRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<LockedFeature> lockedFeatures = List.of(
            LockedFeature.COMMENT,
            LockedFeature.POST,
            LockedFeature.PREDICTION,
            LockedFeature.PROPOSE);

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        JwtPayload jwtPayload = JwtPayload.getCurrentUserPayload();
        Long userId = jwtPayload.getUserId();

        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!userProfile.getUser().isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }

        return buildMyProfileResponse(userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfile(Long userId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        if (Objects.equals(currentUserId, userId)) {
            return getMyProfile();
        }

        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!userProfile.getUser().isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }

        Friendship friendship = friendshipRepository.findFriendshipsBetween(currentUserId, userId)
                .orElse(Friendship.builder()
                        .status(FriendshipStatus.NONE)
                        .build()
                );

        Relationship relationships = Relationship.builder()
                // userId có đang được currentUserId theo dõi không
                .isFollowed(followRepository.existsByFollowerIdAndFollowedId(currentUserId, userId))

                // userId có đang theo dõi currentUserId không
                .isFollowing(followRepository.existsByFollowerIdAndFollowedId(userId, currentUserId))
                .isFriend(friendship.getStatus().equals(FriendshipStatus.ACCEPTED))
                .friendRequestStatus(friendship.getStatus())
                .build();

        return ProfileResponse.builder()
                .id(userProfile.getUser().getId())
                .fullName(userProfile.getFullName())
                .avatarUrl(userProfile.getAvatarUrl())
                .coverUrl(userProfile.getCoverUrl())
                .frameUrl(userProfile.getAvatarFrameUrl())
                .bio(userProfile.getBio())
                .faculty(userProfile.getFaculty())
                .major(userProfile.getMajor())
                .studentLecturerCode(userProfile.getStudentLecturerCode())
                .role(userProfile.getUser().getRole())
                .totalProposedQuestions(userProfile.getTotalProposedQuestion())
                .totalApprovedQuestions(userProfile.getTotalApprovedQuestions())
                .gamificationPoints(userProfile.getGamificationPoints())
                .badgesCount(userProfile.getBadgesCount())
                .friendsCount(userProfile.getFriendsCount())
                .followersCount(userProfile.getFollowersCount())
                .followingCount(userProfile.getFollowingCount())
                .relationships(relationships)
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!userProfile.getUser().isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            userProfile.setFullName(request.fullName().trim());
        }
        if (request.avatarUrl() != null) {
            userProfile.setAvatarUrl(request.avatarUrl());
        }
        if (request.coverUrl() != null) {
            userProfile.setCoverUrl(request.coverUrl());
        }
        if (request.bio() != null) {
            userProfile.setBio(request.bio().trim());
        }
        if (request.faculty() != null) {
            userProfile.setFaculty(request.faculty().trim());
        }
        if (request.major() != null) {
            userProfile.setMajor(request.major().trim());
        }
        if (request.studentLecturerCode() != null) {
            userProfile.setStudentLecturerCode(request.studentLecturerCode().trim());
        }
        userProfile.getUser().setUpdatedAt(LocalDateTime.now());

        // @Transactional tự save
        // userRepository.save(userProfile.getUser()); /
        // userProfileRepository.save(userProfile);

        return buildMyProfileResponse(userProfile);
    }

    private ProfileResponse buildMyProfileResponse(UserProfile userProfile) {

        boolean profileCompleted = userProfile.isProfileCompleted();

        return ProfileResponse.builder()
                .id(userProfile.getUser().getId())
                .email(userProfile.getUser().getEmail())
                .fullName(userProfile.getFullName())
                .avatarUrl(userProfile.getAvatarUrl())
                .coverUrl(userProfile.getCoverUrl())
                .frameUrl(userProfile.getAvatarFrameUrl())
                .bio(userProfile.getBio())
                .faculty(userProfile.getFaculty())
                .major(userProfile.getMajor())
                .studentLecturerCode(userProfile.getStudentLecturerCode())
                .role(userProfile.getUser().getRole())
                .profileCompleted(profileCompleted)
                .verified(userProfile.getUser().isVerified())
                .totalProposedQuestions(userProfile.getTotalProposedQuestion())
                .totalApprovedQuestions(userProfile.getTotalApprovedQuestions())
                .gamificationPoints(userProfile.getGamificationPoints())
                .badgesCount(userProfile.getBadgesCount())
                .friendsCount(userProfile.getFriendsCount())
                .followersCount(userProfile.getFollowersCount())
                .followingCount(userProfile.getFollowingCount())
                .lockedFeatures(profileCompleted ? null : lockedFeatures)
                .createdAt(userProfile.getUser().getCreatedAt())
                .relationships(null)
                .build();
    }

    @Override
    @Transactional
    public void deleteMyProfile() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        User user = userProfile.getUser();

        user.setActive(false);
        user.setEmail("Deleted_Account_" + UUID.randomUUID());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        userProfile.setFullName("Tài khoản đã bị xóa");
        userProfile.setAvatarUrl("");
        userProfile.setCoverUrl("");
        userProfile.setAvatarFrameUrl("");
        userProfile.setBio("");
        userProfile.setFaculty("");
        userProfile.setMajor("");
        userProfile.setStudentLecturerCode("");
        userProfile.setTotalProposedQuestion(0);
        userProfile.setTotalApprovedQuestions(0);
        userProfile.setGamificationPoints(0.0);
        userProfile.setBadgesCount(0);
        userProfile.setFriendsCount(0);
        userProfile.setFollowersCount(0);
        userProfile.setFollowingCount(0);

        user.setUpdatedAt(LocalDateTime.now());

        // @Transactional tự save
        // userRepository.save(user); /
        // userProfileRepository.save(userProfile);
    }

}
