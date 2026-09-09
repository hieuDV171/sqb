package com.frozenheart.backend.modules.notification.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.notification.dto.*;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
import com.frozenheart.backend.modules.notification.service.NotificationService;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

        private final NotificationRepository notificationRepository;
        private final UserProfileRepository userProfileRepository;
        private final UserPushSettingRepository userPushSettingRepository;
        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public NotificationListResponseDto getMyNotifications(Long after, Integer limit) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                Long cursor = (after != null && after > 0) ? after : null;
                List<Notification> notifications = notificationRepository.findNotificationsCursor(currentUserId, cursor,
                                pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (notifications.size() > pageSize) {
                        hasNext = true;
                        notifications = notifications.subList(0, pageSize);
                        nextCursor = notifications.getLast().getId();
                }

                // Triệt tiêu N+1: Batch fetch toàn bộ UserProfile của actors
                Set<Long> actorUserIds = notifications.stream()
                                .map(Notification::getActor)
                                .filter(Objects::nonNull)
                                .map(User::getId)
                                .collect(Collectors.toSet());

                Map<Long, UserProfile> profileMap = actorUserIds.isEmpty() ? Map.of()
                                : userProfileRepository.findAllById(actorUserIds).stream()
                                                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

                List<NotificationDto> items = notifications.stream().map(n -> {
                        User actor = n.getActor();
                        UserSummaryDto actorSummary = null;
                        if (actor != null) {
                                UserProfile profile = profileMap.get(actor.getId());
                                actorSummary = UserSummaryDto.builder()
                                                .userId(actor.getId())
                                                .fullName(profile != null ? profile.getFullName() : actor.getEmail())
                                                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                                                .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                                                .build();
                        }

                        NotificationTargetDto target = NotificationTargetDto.builder()
                                        .type(n.getTargetType())
                                        .id(n.getTargetId())
                                        .url(n.getTargetUrl())
                                        .build();

                        return NotificationDto.builder()
                                        .notificationId(n.getId())
                                        .type(n.getType())
                                        .title(n.getTitle())
                                        .body(n.getBody())
                                        .iconUrl(n.getIconUrl())
                                        .readAt(n.getReadAt())
                                        .createdAt(n.getCreadtedAt())
                                        .target(target)
                                        .actor(actorSummary)
                                        .metadata(n.getMetadata())
                                        .build();
                }).collect(Collectors.toList());

                int unreadCount = notificationRepository.countUnreadNotifications(currentUserId);

                CursorPaginationDto pagination = CursorPaginationDto.builder()
                                .after(nextCursor)
                                .hasNext(hasNext)
                                .build();

                return NotificationListResponseDto.builder()
                                .items(items)
                                .pagination(pagination)
                                .unreadCount(unreadCount)
                                .build();
        }

        @Override
        @Transactional
        public ReadNotificationResponseDto markNotificationAsRead(Long notificationId) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                Notification notification = notificationRepository.findById(notificationId)
                                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND,
                                                "Thông báo không tồn tại"));

                if (!Objects.equals(notification.getReceiver().getId(), currentUserId)) {
                        throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền truy cập thông báo này");
                }

                Instant readAt = notification.getReadAt();
                if (readAt == null) {
                        readAt = Instant.now();
                        notification.setReadAt(readAt);
                        notificationRepository.save(notification);
                }

                int unreadCount = notificationRepository.countUnreadNotifications(currentUserId);

                return ReadNotificationResponseDto.builder()
                                .notificationId(notification.getId())
                                .isRead(true)
                                .readAt(readAt)
                                .unreadCount(unreadCount)
                                .build();
        }

        @Override
        @Transactional
        public void markAllNotificationsAsRead() {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                notificationRepository.markAllAsRead(currentUserId, Instant.now());
        }

        @Override
        @Transactional(readOnly = true)
        public PushSettingsResponseDto getPushSettings() {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                UserPushSetting setting = userPushSettingRepository.findById(currentUserId).orElse(null);
                PushPreferences prefs = (setting != null && setting.getPreferences() != null)
                                ? setting.getPreferences()
                                : PushPreferences.createDefault();

                Map<String, PushCategorySettingDto> categories = new LinkedHashMap<>();
                if (prefs.getCategories() != null) {
                        for (Map.Entry<PushNotificationType, CategorySetting> entry : prefs.getCategories()
                                        .entrySet()) {
                                CategorySetting cs = entry.getValue();
                                categories.put(entry.getKey().name().toLowerCase(),
                                                new PushCategorySettingDto(cs.isEnabled(), cs.getDescription()));
                        }
                }

                QuietHoursDto quietHoursDto;
                if (prefs.getQuietHour() != null) {
                        QuietHour qh = prefs.getQuietHour();
                        String startTime = qh.getStartTime() != null
                                        ? qh.getStartTime().format(Time.TIME_FORMATTER_HH_MM)
                                        : "22:00";
                        String endTime = qh.getEndTime() != null ? qh.getEndTime().format(Time.TIME_FORMATTER_HH_MM)
                                        : "07:00";
                        quietHoursDto = QuietHoursDto.builder()
                                        .enabled(qh.isEnabled())
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .build();
                } else {
                        quietHoursDto = QuietHoursDto.builder()
                                        .enabled(false)
                                        .startTime("22:00")
                                        .endTime("07:00")
                                        .build();
                }

                return PushSettingsResponseDto.builder()
                                .pushEnabled(prefs.isPushEnabled())
                                .categories(categories)
                                .quietHours(quietHoursDto)
                                .soundEnabled(prefs.isSoundEnabled())
                                .vibrationEnabled(prefs.isVibrationEnabled())
                                .build();
        }

        @Override
        @Transactional
        public UpdatePushSettingsResponseDto updatePushSettings(UpdatePushSettingsRequestDto request) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                User currentUser = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

                UserPushSetting setting = userPushSettingRepository.findById(currentUserId)
                                .orElseGet(() -> UserPushSetting.builder()
                                                .userId(currentUserId)
                                                .user(currentUser)
                                                .build());

                Map<PushNotificationType, CategorySetting> categories = new EnumMap<>(PushNotificationType.class);
                if (request.getCategories() != null) {
                        for (Map.Entry<String, PushCategorySettingDto> entry : request.getCategories().entrySet()) {
                                try {
                                        PushNotificationType type = PushNotificationType
                                                        .valueOf(entry.getKey().toUpperCase());
                                        PushCategorySettingDto dto = entry.getValue();
                                        categories.put(type,
                                                        new CategorySetting(dto.isEnabled(), type.getDescription()));
                                } catch (IllegalArgumentException ignored) {
                                        log.warn("Unknown push category type: {}", entry.getKey());
                                }
                        }
                }

                QuietHour quietHour = null;
                if (request.getQuietHours() != null) {
                        QuietHoursDto qhDto = request.getQuietHours();
                        LocalTime startTime = (qhDto.getStartTime() != null && !qhDto.getStartTime().isBlank())
                                        ? LocalTime.parse(qhDto.getStartTime(), Time.TIME_FORMATTER_HH_MM)
                                        : LocalTime.of(22, 0);
                        LocalTime endTime = (qhDto.getEndTime() != null && !qhDto.getEndTime().isBlank())
                                        ? LocalTime.parse(qhDto.getEndTime(), Time.TIME_FORMATTER_HH_MM)
                                        : LocalTime.of(7, 0);

                        quietHour = QuietHour.builder()
                                        .enabled(qhDto.isEnabled())
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .build();
                }

                PushPreferences preferences = PushPreferences.builder()
                                .pushEnabled(request.isPushEnabled())
                                .categories(categories)
                                .quietHour(quietHour)
                                .soundEnabled(request.isSoundEnabled())
                                .vibrationEnabled(request.isVibrationEnabled())
                                .build();

                setting.setPreferences(preferences);
                setting.setUpdatedAt(Instant.now());
                userPushSettingRepository.save(setting);

                return UpdatePushSettingsResponseDto.builder()
                                .pushEnabled(request.isPushEnabled())
                                .effectiveFrom(Instant.now())
                                .build();
        }
}
