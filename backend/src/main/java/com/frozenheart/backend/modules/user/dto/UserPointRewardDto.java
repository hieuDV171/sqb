package com.frozenheart.backend.modules.user.dto;

import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.session.Subject;
import lombok.Builder;

@Builder
public record UserPointRewardDto(
        Long userId,
        double pointsDelta,
        int approvedDelta,
        PointHistoryReason reason,
        PointHistoryTargetType targetType,
        Long targetId,
        Subject subject
) {}
