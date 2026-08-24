package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.session.Subject;
import lombok.Builder;

@Builder
public record UserPointRewardDto(
        Long userId,
        double pointsDelta,
        int approvedDelta,
        String reason,
        String targetType,
        Long targetId,
        Subject subject
) {}
