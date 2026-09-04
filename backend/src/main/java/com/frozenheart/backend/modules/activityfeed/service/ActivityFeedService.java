package com.frozenheart.backend.modules.activityfeed.service;

import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedMetaData;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.activityfeed.dto.*;

public interface ActivityFeedService {

    CursorResponse<ActivityFeedItemDto> getActivityFeeds(Long after, Integer limit);

    NewFeedCountResponseDto getNewFeedCount(Long since);

    void logActivity(User actor, ActionType actionType, String targetType, Long targetId, ActivityFeedMetaData metaData);
}
