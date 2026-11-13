package com.frozenheart.backend.modules.follow.service;

import com.frozenheart.backend.modules.follow.dto.*;

public interface FollowService {

    FollowResponseDto followUser(Long targetUserId);

    UnfollowResponseDto unfollowUser(Long targetUserId);

    FollowingListResponseDto getFollowing(Long after, String cursor, Integer limit);

    FollowerListResponseDto getFollowers(Long after, String cursor, Integer limit);

    RelationshipStatsResponseDto getRelationshipStats(Long userId);
}
