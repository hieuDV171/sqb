package com.frozenheart.backend.modules.follow.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.follow.dto.*;
import com.frozenheart.backend.modules.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follows/{targetUserId}")
    public ResponseEntity<GlobalResponse<FollowResponseDto>> followUser(@PathVariable Long targetUserId) {
        FollowResponseDto response = followService.followUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @DeleteMapping("/follows/{targetUserId}")
    public ResponseEntity<GlobalResponse<UnfollowResponseDto>> unfollowUser(@PathVariable Long targetUserId) {
        UnfollowResponseDto response = followService.unfollowUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/follows/following")
    public ResponseEntity<GlobalResponse<FollowingListResponseDto>> getFollowing(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FollowingListResponseDto response = followService.getFollowing(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/follows/followers")
    public ResponseEntity<GlobalResponse<FollowerListResponseDto>> getFollowers(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FollowerListResponseDto response = followService.getFollowers(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/users/{userId}/relationship-stats")
    public ResponseEntity<GlobalResponse<RelationshipStatsResponseDto>> getRelationshipStats(@PathVariable Long userId) {
        RelationshipStatsResponseDto response = followService.getRelationshipStats(userId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
