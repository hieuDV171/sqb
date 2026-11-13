package com.frozenheart.backend.modules.friendship.service;

import com.frozenheart.backend.modules.friendship.dto.*;

public interface FriendshipService {

    FriendshipResponseDto sendFriendRequest(SendFriendRequestDto request);

    FriendshipResponseDto acceptFriendRequest(AcceptFriendRequestDto request);

    void declineFriendRequest(DeclineFriendRequestDto request);

    void cancelSentRequest(Long receiverId);

    void unfriend(Long friendId);

    FriendListResponseDto getMyFriends(Long after, String cursor, Integer limit);

    FriendRequestReceivedListResponseDto getReceivedFriendRequests(Long after, String cursor, Integer limit);

    FriendRequestSentListResponseDto getSentFriendRequests(Long after, String cursor, Integer limit);
}
