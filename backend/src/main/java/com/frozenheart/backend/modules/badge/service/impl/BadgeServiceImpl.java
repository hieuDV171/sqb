package com.frozenheart.backend.modules.badge.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedMetaData;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedTargetType;
import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;
import com.frozenheart.backend.core.entity.badge.UserBadge;
import com.frozenheart.backend.core.entity.badge.UserBadgeId;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.dto.event.MediaCleanupEvent;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.badge.dto.*;
import com.frozenheart.backend.modules.badge.repository.BadgeRepository;
import com.frozenheart.backend.modules.badge.repository.UserBadgeRepository;
import com.frozenheart.backend.modules.badge.service.BadgeEvaluationService;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.notification.NotificationCategory;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final NotificationWriter notificationWriter;
    private final BadgeEvaluationService badgeEvaluationService;
    private final UserProfileRepository userProfileRepository;
    private final ActivityFeedService activityFeedService;
    private final MediaService mediaService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponseDto> getAllBadges() {
        return mapBadgeList(badgeRepository.findByActiveTrueOrderByIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponseDto> getAdminBadges() {
        return mapBadgeList(badgeRepository.findAll(Sort.by("id")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeResponseDto> getMyBadges() {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        return getUserBadges(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeResponseDto> getUserBadges(Long userId) {
        return userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .map(this::mapToUserBadgeResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BadgeResponseDto getBadgeById(Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new AppException(ResponseCode.BADGE_NOT_FOUND));
        return mapToBadgeResponseDto(badge);
    }

    @Override
    @Transactional
    public BadgeResponseDto createBadge(CreateBadgeRequestDto request) {
        if (badgeRepository.existsByName(request.getName())) {
            throw new AppException(ResponseCode.BADGE_NAME_ALREADY_EXISTS);
        }

        if (request.getIconUrl() != null && !request.getIconUrl().isBlank()) {
            mediaService.confirmMediaPermanent(List.of(request.getIconUrl()));
        }

        Badge badge = Badge.builder()
                .name(request.getName())
                .description(request.getDescription())
                .badgeTier(request.getBadgeTier())
                .badgeTriggerEvent(request.getBadgeTriggerEvent())
                .iconUrl(request.getIconUrl())
                .criteria(request.getCriteria())
                .active(true)
                .createdAt(Instant.now())
                .build();

        Badge saved = badgeRepository.save(badge);
        log.info("[BadgeService] Tạo huy hiệu mới thành công: {} (ID: {})", saved.getName(), saved.getId());
        return mapToBadgeResponseDto(saved);
    }

    @Override
    @Transactional
    public BadgeResponseDto updateBadge(Long badgeId, UpdateBadgeRequestDto request) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new AppException(ResponseCode.BADGE_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!badge.getName().equalsIgnoreCase(request.getName())
                    && badgeRepository.existsByName(request.getName())) {
                throw new AppException(ResponseCode.BADGE_NAME_ALREADY_EXISTS);
            }
            badge.setName(request.getName());
        }

        if (request.getDescription() != null) {
            badge.setDescription(request.getDescription());
        }
        if (request.getBadgeTier() != null) {
            badge.setBadgeTier(request.getBadgeTier());
        }
        if (request.getBadgeTriggerEvent() != null) {
            badge.setBadgeTriggerEvent(request.getBadgeTriggerEvent());
        }
        if (request.getActive() != null) {
            badge.setActive(request.getActive());
        }
        if (request.getIconUrl() != null) {
            String oldIcon = badge.getIconUrl();
            if (!request.getIconUrl().isBlank()) {
                mediaService.confirmMediaPermanent(List.of(request.getIconUrl()));
            }
            badge.setIconUrl(request.getIconUrl());
            if (oldIcon != null && !oldIcon.isBlank() && !oldIcon.equals(request.getIconUrl())) {
                eventPublisher.publishEvent(MediaCleanupEvent.of(oldIcon));
            }
        }
        if (request.getCriteria() != null) {
            badge.setCriteria(request.getCriteria());
        }

        Badge updated = badgeRepository.save(badge);
        return mapToBadgeResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteBadge(Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new AppException(ResponseCode.BADGE_NOT_FOUND));
        // Soft delete để bảo toàn toàn vẹn dữ liệu cho sinh viên đã đạt huy hiệu trong
        // quá khứ
        badge.setActive(false);
        badgeRepository.save(badge);
        log.info("[BadgeService] Đã vô hiệu hóa (Soft Delete) huy hiệu: {} (ID: {})", badge.getName(), badgeId);
    }

    @Override
    @Transactional
    public void grantManualBadge(Long badgeId, ManualGrantBadgeRequestDto request) {
        Badge badge = badgeRepository.findByIdForGrant(badgeId)
                .orElseThrow(() -> new AppException(ResponseCode.BADGE_NOT_FOUND));
        if (!badge.isActive()) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Huy hiệu đã ngừng hoạt động.");
        }

        List<Long> requestedUserIds = request.getUserIds().stream().distinct().toList();
        List<User> users = userRepository.findAllById(requestedUserIds);
        if (users.size() != requestedUserIds.size() || users.stream().anyMatch(user -> !user.isActive())) {
            throw new AppException(ResponseCode.USER_NOT_FOUND, "Có tài khoản không tồn tại hoặc đã bị vô hiệu hóa.");
        }
        Set<Long> alreadyGrantedIds = userBadgeRepository.findGrantedUserIds(badgeId, requestedUserIds);
        List<User> recipients = users.stream().filter(user -> !alreadyGrantedIds.contains(user.getId())).toList();
        if (recipients.isEmpty()) return;
        Instant now = Instant.now();
        String reason = request.getReason() != null && !request.getReason().isBlank() ? request.getReason() : badge.getDescription();
        recipients.forEach(user -> entityManager.persist(newAward(user, badge, now)));
        userProfileRepository.incrementBadgesCountForUsers(recipients.stream().map(User::getId).toList());
        var metadata = badgeFeedMetadata(badge, reason);
        activityFeedService.logActivities(recipients.stream().map(user -> ActivityFeed.builder().user(user)
                .actionType(ActionType.EARNED_BADGE).targetType(ActivityFeedTargetType.BADGE).targetId(badgeId)
                .metaData(metadata).createdAt(now).build()).toList());
        notificationWriter.createAll(recipients.stream().map(user -> badgeNotification(user, badge, reason, now)).toList());

        log.info("[BadgeService] Đã trao huy hiệu '{}' (ID: {}) thành công theo lô cho {} người dùng",
                badge.getName(), badge.getId(), recipients.size());
    }

    @Override
    @Transactional
    public void checkAndGrantBadges(User user, BadgeTriggerEvent badgeTriggerEvent, Object context) {
        if (user == null || badgeTriggerEvent == null) {
            return;
        }

        // Lọc trực tiếp từ database: chỉ lấy các huy hiệu thuộc badgeTriggerEvent mà
        // user chưa đạt
        List<Badge> targetBadges = badgeRepository.findUnearnedActiveBadgesByUserAndBadgeTriggerEvent(user.getId(),
                badgeTriggerEvent);
        for (Badge badge : targetBadges) {
            if (badgeEvaluationService.isEligible(user, badge, context)) {
                grantBadgeToUser(user, badge, null);
            }
        }
    }

    @Override
    @Transactional
    public void checkAndGrantBadgesBatch(List<User> users, BadgeTriggerEvent badgeTriggerEvent, Object context) {
        if (users == null || users.isEmpty() || badgeTriggerEvent == null) {
            return;
        }

        List<Badge> activeBadges = badgeRepository.findByActiveTrueAndBadgeTriggerEvent(badgeTriggerEvent);
        if (activeBadges.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(User::getId).filter(Objects::nonNull).distinct().toList();
        List<Long> badgeIds = activeBadges.stream().map(Badge::getId).toList();

        List<UserBadge> existingUserBadges = userBadgeRepository.findByUserIdsAndBadgeIds(userIds, badgeIds);
        Set<String> ownedKeys = existingUserBadges.stream()
                .map(ub -> ub.getId().getUserId() + "_" + ub.getId().getBadgeId())
                .collect(Collectors.toSet());

        for (User user : users) {
            if (user == null || user.getId() == null)
                continue;

            Object userContext = (context instanceof Map<?, ?> contextMap)
                    ? contextMap.get(user.getId())
                    : context;

            for (Badge badge : activeBadges) {
                String key = user.getId() + "_" + badge.getId();
                if (!ownedKeys.contains(key)) {
                    if (badgeEvaluationService.isEligible(user, badge, userContext)) {
                        grantBadgeToUser(user, badge, null);
                        ownedKeys.add(key);
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void checkAndGrantSpecificBadge(User user, Long badgeId, Object context) {
        if (user == null || badgeId == null) {
            return;
        }

        if (userBadgeRepository.existsByIdUserIdAndIdBadgeId(user.getId(), badgeId)) {
            return;
        }

        Badge badge = badgeRepository.findById(badgeId).orElse(null);
        if (badge != null && badge.isActive() && badgeEvaluationService.isEligible(user, badge, context)) {
            grantBadgeToUser(user, badge, null);
        }
    }

    // TODO: Hệ thống chưa có milestone
    @Override
    @Transactional
    public void checkAndLogMilestone(User user, String milestoneTitle, String description, Long targetId) {
        if (user == null) {
            return;
        }

        ActivityFeedMetaData feedMeta = ActivityFeedMetaData.builder()
                .title(milestoneTitle)
                .description(description)
                .build();

        activityFeedService.logActivity(
                user,
                ActionType.REACHED_MILESTONE,
                ActivityFeedTargetType.USER,
                targetId != null ? targetId : user.getId(),
                feedMeta);
        log.info("[BadgeService] Ghi nhận cột mốc cho user {}: {}", user.getId(), milestoneTitle);
    }

    private void grantBadgeToUser(User user, Badge badge, String customReason) {
        Instant now = Instant.now();
        UserBadge userBadge = newAward(user, badge, now);
        if (userBadgeRepository.existsById(userBadge.getId())) {
            return;
        }
        userBadgeRepository.save(userBadge);
        userProfileRepository.incrementBadgesCount(user.getId(), 1);
        String reason = customReason != null ? customReason : badge.getDescription();
        activityFeedService.logActivity(user, ActionType.EARNED_BADGE, ActivityFeedTargetType.BADGE, badge.getId(),
                badgeFeedMetadata(badge, reason));
        notificationWriter.create(badgeNotification(user, badge, reason, now));
        log.info("[BadgeService] Trao huy hiệu '{}' (ID: {}) cho User {} thành công!", badge.getName(), badge.getId(), user.getId());
    }

    private UserBadge newAward(User user, Badge badge, Instant now) {
        return UserBadge.builder().id(UserBadgeId.builder().userId(user.getId()).badgeId(badge.getId()).build())
                .user(user).badge(badge).earnedAt(now).build();
    }

    private ActivityFeedMetaData badgeFeedMetadata(Badge badge, String reason) {
        return ActivityFeedMetaData.builder()
                .title("Đạt huy hiệu mới: " + badge.getName())
                .description(reason)
                .badgeName(badge.getName())
                .mediaUrl(badge.getIconUrl())
                .build();
    }

    private Notification badgeNotification(User user, Badge badge, String reason, Instant now) {
        String body = reason;
        if (reason != null && reason.codePointCount(0, reason.length()) > 255) {
            body = reason.substring(0, reason.offsetByCodePoints(0, 254)) + "…";
        }
        return Notification.builder()
                .receiver(user)
                .actor(null)
                .type(NotificationType.EARNED_BADGE)
                .category(NotificationCategory.GAMIFICATION)
                .title("Chúc mừng bạn đã nhận được huy hiệu mới: " + badge.getName())
                .body(body)
                .iconUrl(badge.getIconUrl())
                .targetType(NotificationTargetType.BADGE)
                .targetId(badge.getId())
                .creadtedAt(now)
                .build();
    }

    private List<BadgeResponseDto> mapBadgeList(List<Badge> badges) {
        if (badges.isEmpty()) return List.of();
        var counts = new HashMap<Long, Integer>();
        userBadgeRepository.countGroupedByBadgeId(badges.stream().map(Badge::getId).toList())
                .forEach(count -> counts.put(count.getBadgeId(), count.getEarnedCount().intValue()));
        return badges.stream().map(badge -> mapToBadgeResponseDto(badge, counts.getOrDefault(badge.getId(), 0))).toList();
    }

    private BadgeResponseDto mapToBadgeResponseDto(Badge badge) {
        return mapToBadgeResponseDto(badge, userBadgeRepository.countByBadgeId(badge.getId()));
    }

    private BadgeResponseDto mapToBadgeResponseDto(Badge badge, int totalEarned) {
        return BadgeResponseDto.builder()
                .badgeId(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .badgeTier(badge.getBadgeTier())
                .badgeTriggerEvent(badge.getBadgeTriggerEvent())
                .iconUrl(badge.getIconUrl())
                .criteria(badge.getCriteria())
                .totalEarnedUsers(totalEarned)
                .active(badge.isActive())
                .createdAt(badge.getCreatedAt())
                .build();
    }

    private UserBadgeResponseDto mapToUserBadgeResponseDto(UserBadge userBadge) {
        Badge badge = userBadge.getBadge();
        return UserBadgeResponseDto.builder()
                .badgeId(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .badgeTier(badge.getBadgeTier())
                .iconUrl(badge.getIconUrl())
                .earnedAt(userBadge.getEarnedAt())
                .build();
    }
}
