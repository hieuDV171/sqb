import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  ProfileResponse,
  UpdateProfileRequest,
  FriendListResponseDto,
  FriendRequestReceivedListResponseDto,
  FriendRequestSentListResponseDto,
  FriendshipResponseDto,
  FollowingListResponseDto,
  FollowerListResponseDto,
  FollowResponseDto,
  UnfollowResponseDto,
  RelationshipStatsResponseDto,
  BlockedListResponseDto,
  BlockResponseDto,
  UnblockResponseDto,
  SendFriendRequestDto,
  AcceptFriendRequestDto,
  DeclineFriendRequestDto,
} from '@/types/user.types';

export const userService = {
  // ==================== USER PROFILE ====================
  getMyProfile: async (): Promise<GlobalResponse<ProfileResponse>> => {
    return (await axiosClient.get<GlobalResponse<ProfileResponse>>('/users/me')) as any;
  },

  getUserProfile: async (userId: number): Promise<GlobalResponse<ProfileResponse>> => {
    return (await axiosClient.get<GlobalResponse<ProfileResponse>>(`/users/${userId}`)) as any;
  },

  updateMyProfile: async (
    data: UpdateProfileRequest
  ): Promise<GlobalResponse<ProfileResponse>> => {
    return (await axiosClient.put<GlobalResponse<ProfileResponse>>('/users/me', data)) as any;
  },

  deleteMyProfile: async (): Promise<GlobalResponse<void>> => {
    return (await axiosClient.delete<GlobalResponse<void>>('/users/me')) as any;
  },

  // ==================== FRIENDSHIPS ====================
  getMyFriends: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<FriendListResponseDto>>('/friendships', {
      params: { after, limit },
    })) as any;
  },

  getReceivedFriendRequests: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendRequestReceivedListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<FriendRequestReceivedListResponseDto>>(
      '/friendships/requests/received',
      { params: { after, limit } }
    )) as any;
  },

  getSentFriendRequests: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendRequestSentListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<FriendRequestSentListResponseDto>>(
      '/friendships/requests/sent',
      { params: { after, limit } }
    )) as any;
  },

  sendFriendRequest: async (
    data: SendFriendRequestDto
  ): Promise<GlobalResponse<FriendshipResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<FriendshipResponseDto>>(
      '/friendships/request',
      data
    )) as any;
  },

  acceptFriendRequest: async (
    data: AcceptFriendRequestDto
  ): Promise<GlobalResponse<FriendshipResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<FriendshipResponseDto>>(
      '/friendships/accept',
      data
    )) as any;
  },

  declineFriendRequest: async (
    data: DeclineFriendRequestDto
  ): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      '/friendships/decline',
      data
    )) as any;
  },

  unfriend: async (friendId: number): Promise<GlobalResponse<void>> => {
    return (await axiosClient.delete<GlobalResponse<void>>(`/friendships/${friendId}`)) as any;
  },

  // ==================== FOLLOWS ====================
  getFollowing: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FollowingListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<FollowingListResponseDto>>('/follows/following', {
      params: { after, limit },
    })) as any;
  },

  getFollowers: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FollowerListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<FollowerListResponseDto>>('/follows/followers', {
      params: { after, limit },
    })) as any;
  },

  getUserRelationshipStats: async (
    userId: number
  ): Promise<GlobalResponse<RelationshipStatsResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<RelationshipStatsResponseDto>>(
      `/users/${userId}/relationship-stats`
    )) as any;
  },

  followUser: async (targetUserId: number): Promise<GlobalResponse<FollowResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<FollowResponseDto>>(
      `/follows/${targetUserId}`
    )) as any;
  },

  unfollowUser: async (targetUserId: number): Promise<GlobalResponse<UnfollowResponseDto>> => {
    return (await axiosClient.delete<GlobalResponse<UnfollowResponseDto>>(
      `/follows/${targetUserId}`
    )) as any;
  },

  // ==================== BLOCKS ====================
  blockUser: async (targetUserId: number): Promise<GlobalResponse<BlockResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<BlockResponseDto>>(
      `/blocks/${targetUserId}`
    )) as any;
  },

  unblockUser: async (targetUserId: number): Promise<GlobalResponse<UnblockResponseDto>> => {
    return (await axiosClient.delete<GlobalResponse<UnblockResponseDto>>(
      `/blocks/${targetUserId}`
    )) as any;
  },

  getBlockedUsers: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<BlockedListResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<BlockedListResponseDto>>('/blocks', {
      params: { after, limit },
    })) as any;
  },
};
