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
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendListResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<FriendListResponseDto>>('/friendships', {
      params: {
        cursor: typeof cursor === 'string' ? cursor : undefined,
        after: typeof cursor === 'number' ? cursor : undefined,
        limit,
      },
    });
  },

  getReceivedFriendRequests: async (
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendRequestReceivedListResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<FriendRequestReceivedListResponseDto>>(
      '/friendships/requests/received',
      {
        params: {
          cursor: typeof cursor === 'string' ? cursor : undefined,
          after: typeof cursor === 'number' ? cursor : undefined,
          limit,
        },
      }
    );
  },

  getSentFriendRequests: async (
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendRequestSentListResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<FriendRequestSentListResponseDto>>(
      '/friendships/requests/sent',
      {
        params: {
          cursor: typeof cursor === 'string' ? cursor : undefined,
          after: typeof cursor === 'number' ? cursor : undefined,
          limit,
        },
      }
    );
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

  cancelSentFriendRequest: (receiverId: number): Promise<GlobalResponse<void>> =>
    axiosClient.delete<GlobalResponse<void>, GlobalResponse<void>>(`/friendships/requests/sent/${receiverId}`, {
      skipToast: true,
    }),

  // ==================== FOLLOWS ====================
  getFollowing: async (
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<FollowingListResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<FollowingListResponseDto>>('/follows/following', {
      params: {
        cursor: typeof cursor === 'string' ? cursor : undefined,
        after: typeof cursor === 'number' ? cursor : undefined,
        limit,
      },
    });
  },

  getFollowers: async (
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<FollowerListResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<FollowerListResponseDto>>('/follows/followers', {
      params: {
        cursor: typeof cursor === 'string' ? cursor : undefined,
        after: typeof cursor === 'number' ? cursor : undefined,
        limit,
      },
    });
  },

  getUserRelationshipStats: async (
    userId: number
  ): Promise<GlobalResponse<RelationshipStatsResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<RelationshipStatsResponseDto>>(
      `/users/${userId}/relationship-stats`
    )) as any;
  },

  followUser: async (targetUserId: number): Promise<GlobalResponse<FollowResponseDto>> => {
    return axiosClient.post<GlobalResponse<FollowResponseDto>, GlobalResponse<FollowResponseDto>>(
      `/follows/${targetUserId}`, undefined, { skipToast: true },
    );
  },

  unfollowUser: async (targetUserId: number): Promise<GlobalResponse<UnfollowResponseDto>> => {
    return axiosClient.delete<GlobalResponse<UnfollowResponseDto>, GlobalResponse<UnfollowResponseDto>>(
      `/follows/${targetUserId}`, { skipToast: true },
    );
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
    cursor?: string | number,
    limit: number = 20
  ): Promise<GlobalResponse<BlockedListResponseDto>> => {
    return axiosClient.get<unknown, GlobalResponse<BlockedListResponseDto>>('/blocks', {
      params: {
        cursor: typeof cursor === 'string' ? cursor : undefined,
        after: typeof cursor === 'number' ? cursor : undefined,
        limit,
      },
    });
  },
};
