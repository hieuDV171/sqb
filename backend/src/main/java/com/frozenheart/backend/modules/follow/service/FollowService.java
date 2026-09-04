package com.frozenheart.backend.modules.follow.service;

import com.frozenheart.backend.modules.follow.dto.*;

public interface FollowService {

    FollowResponseDto followUser(Long targetUserId);

    UnfollowResponseDto unfollowUser(Long targetUserId);

    FollowingListResponseDto getFollowing(Long after, Integer limit);

    FollowerListResponseDto getFollowers(Long after, Integer limit);

    RelationshipStatsResponseDto getRelationshipStats(Long userId);
}
