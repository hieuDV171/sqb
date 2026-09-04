package com.frozenheart.backend.modules.activityfeed.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.activityfeed.dto.*;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;

    @GetMapping("/activity-feeds")
    public ResponseEntity<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> getActivityFeeds(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        CursorResponse<ActivityFeedItemDto> response = activityFeedService.getActivityFeeds(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/activity-feeds/new-count")
    public ResponseEntity<GlobalResponse<NewFeedCountResponseDto>> getNewFeedCount(
            @RequestParam(required = false) Long since) {
        NewFeedCountResponseDto response = activityFeedService.getNewFeedCount(since);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
