package com.frozenheart.backend.modules.badge.service;

import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.badge.dto.*;

import java.util.List;

public interface BadgeService {

    List<BadgeResponseDto> getAllBadges();

    List<BadgeResponseDto> getAdminBadges();

    List<UserBadgeResponseDto> getMyBadges();

    List<UserBadgeResponseDto> getUserBadges(Long userId);

    BadgeResponseDto getBadgeById(Long badgeId);

    BadgeResponseDto createBadge(CreateBadgeRequestDto request);

    BadgeResponseDto updateBadge(Long badgeId, UpdateBadgeRequestDto request);

    void deleteBadge(Long badgeId);

    void grantManualBadge(Long badgeId, ManualGrantBadgeRequestDto request);

    void checkAndGrantBadges(User user, BadgeTriggerEvent badgeTriggerEvent, Object context);

    void checkAndGrantBadgesBatch(List<User> users, BadgeTriggerEvent badgeTriggerEvent, Object context);

    void checkAndGrantSpecificBadge(User user, Long badgeId, Object context);

    void checkAndLogMilestone(User user, String milestoneTitle, String description, Long targetId);
}
