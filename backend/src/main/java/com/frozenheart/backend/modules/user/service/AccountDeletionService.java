package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.event.AccountDeletedEvent;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.dto.event.MediaCleanupEvent;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserGamification;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.user.dto.AccountDeletionResponse;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final int GRACE_DAYS = 30;
    private static final PageRequest DELETION_BATCH = PageRequest.of(0, 50);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AccountDeletionResponse getStatus(Long userId) {
        User user = requireActive(userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND)));
        return deletionStatus(user);
    }

    @Transactional
    public AccountDeletionResponse request(Long userId) {
        User user = lockActiveAccount(userId);
        if (user.getDeletionDueAt() == null) {
            user.setDeletionDueAt(Instant.now().truncatedTo(ChronoUnit.MILLIS).plus(GRACE_DAYS, ChronoUnit.DAYS));
        }
        return deletionStatus(user);
    }

    @Transactional
    public AccountDeletionResponse cancel(Long userId) {
        User user = lockActiveAccount(userId);
        if (user.getDeletionDueAt() != null && !user.getDeletionDueAt().isAfter(Instant.now())) {
            throw new AppException(ResponseCode.ACCOUNT_DELETION_CANCELLATION_EXPIRED,
                    "Đã hết thời hạn hủy yêu cầu xóa tài khoản.");
        }
        user.setDeletionDueAt(null);
        return new AccountDeletionResponse(null, false);
    }

    @Transactional
    public void processDueAccounts(Instant cutoff) {
        List<User> users = userRepository.findDueAccountsForDeletion(cutoff, DELETION_BATCH);
        if (users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        List<String> mediaToDelete = new ArrayList<>();
        userProfileRepository.findAllById(userIds).forEach(profile -> anonymizeProfile(profile, mediaToDelete));
        userGamificationRepository.findAllById(userIds).forEach(gamification -> resetGamification(gamification, cutoff));
        userDeviceRepository.deactivateForAccounts(userIds);

        for (User user : users) {
            user.setActive(false);
            user.setVerified(false);
            user.setEmail("Deleted_Account_" + UUID.randomUUID());
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setDeletionDueAt(null);
            user.setUpdatedAt(cutoff);
        }
        eventPublisher.publishEvent(new EntitySearchSyncEvent(EntitySearchSyncEvent.EntityType.USER, userIds,
                EntitySearchSyncEvent.SyncAction.DELETE));
        eventPublisher.publishEvent(new AccountDeletedEvent(userIds));
        if (!mediaToDelete.isEmpty()) {
            eventPublisher.publishEvent(MediaCleanupEvent.of(mediaToDelete));
        }
    }

    private User lockActiveAccount(Long userId) {
        return requireActive(userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND)));
    }

    private static AccountDeletionResponse deletionStatus(User user) {
        Instant dueAt = user.getDeletionDueAt();
        return new AccountDeletionResponse(dueAt, dueAt != null && dueAt.isAfter(Instant.now()));
    }

    private static User requireActive(User user) {
        if (!user.isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }
        return user;
    }

    private static void anonymizeProfile(UserProfile profile, List<String> mediaToDelete) {
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            mediaToDelete.add(profile.getAvatarUrl());
        }
        if (profile.getCoverUrl() != null && !profile.getCoverUrl().isBlank()) {
            mediaToDelete.add(profile.getCoverUrl());
        }
        profile.setFullName("Tài khoản đã bị xóa");
        profile.setAvatarUrl("");
        profile.setCoverUrl("");
        profile.setAvatarFrameUrl("");
        profile.setChatBubbleUrl("");
        profile.setBio("");
        profile.setSchoolFaculty("");
        profile.setMajor("");
        profile.setClassName("");
        profile.setStudentLecturerCode("");
        profile.setDateOfBirth(null);
        profile.setGender(null);
        profile.setHiddenChatPin(null);
        profile.setProfileCompleted(false);
        profile.setTotalProposedQuestion(0);
        profile.setTotalApprovedQuestions(0);
        profile.setBadgesCount(0);
        profile.setFriendsCount(0);
        profile.setFollowersCount(0);
        profile.setFollowingCount(0);
    }

    private static void resetGamification(UserGamification gamification, Instant cutoff) {
        gamification.setPublicPoints(0);
        gamification.setSecretPoints(0);
        gamification.setCoinBalance(0);
        gamification.setCurrentStreak(0);
        gamification.setLastCheckInDate(null);
        gamification.setUpdatedAt(cutoff);
    }
}
