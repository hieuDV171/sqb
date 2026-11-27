import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  ProfileResponse,
  AccountDeletionResponse,
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
    return axiosClient.get<unknown, GlobalResponse<ProfileResponse>>('/users/me');
  },

  getUserProfile: async (userId: number): Promise<GlobalResponse<ProfileResponse>> => {
    return axiosClient.get<unknown, GlobalResponse<ProfileResponse>>(`/users/${userId}`);
  },

  updateMyProfile: async (
    data: UpdateProfileRequest
  ): Promise<GlobalResponse<ProfileResponse>> => {
    return axiosClient.put<unknown, GlobalResponse<ProfileResponse>>('/users/me', data);
  },

  getAccountDeletion: (signal?: AbortSignal) =>
    axiosClient.get<unknown, GlobalResponse<AccountDeletionResponse>>('/users/me/deletion', { signal }),

  requestAccountDeletion: () =>
    axiosClient.delete<unknown, GlobalResponse<AccountDeletionResponse>>('/users/me'),

  cancelAccountDeletion: () =>
    axiosClient.delete<unknown, GlobalResponse<AccountDeletionResponse>>('/users/me/deletion'),

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
    return axiosClient.post<unknown, GlobalResponse<FriendshipResponseDto>>(
      '/friendships/request',
      data
    );
  },

  acceptFriendRequest: async (
    data: AcceptFriendRequestDto
  ): Promise<GlobalResponse<FriendshipResponseDto>> => {
    return axiosClient.post<unknown, GlobalResponse<FriendshipResponseDto>>(
      '/friendships/accept',
      data
    );
  },

  declineFriendRequest: async (
    data: DeclineFriendRequestDto
  ): Promise<GlobalResponse<void>> => {
    return axiosClient.post<unknown, GlobalResponse<void>>(
      '/friendships/decline',
      data
    );
  },

  unfriend: async (friendId: number): Promise<GlobalResponse<void>> => {
    return axiosClient.delete<unknown, GlobalResponse<void>>(`/friendships/${friendId}`);
  },

  cancelSentFriendRequest: (receiverId: number): Promise<GlobalResponse<void>> =>
    axiosClient.delete<GlobalResponse<void>, GlobalResponse<void>>(`/friendships/requests/sent/${receiverId}`),

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
    return axiosClient.get<unknown, GlobalResponse<RelationshipStatsResponseDto>>(
      `/users/${userId}/relationship-stats`
    );
  },

  followUser: async (targetUserId: number): Promise<GlobalResponse<FollowResponseDto>> => {
    return axiosClient.post<GlobalResponse<FollowResponseDto>, GlobalResponse<FollowResponseDto>>(
      `/follows/${targetUserId}`, undefined,
    );
  },

  unfollowUser: async (targetUserId: number): Promise<GlobalResponse<UnfollowResponseDto>> => {
    return axiosClient.delete<GlobalResponse<UnfollowResponseDto>, GlobalResponse<UnfollowResponseDto>>(
      `/follows/${targetUserId}`,
    );
  },

  // ==================== BLOCKS ====================
  blockUser: async (targetUserId: number): Promise<GlobalResponse<BlockResponseDto>> => {
    return axiosClient.post<unknown, GlobalResponse<BlockResponseDto>>(
      `/blocks/${targetUserId}`
    );
  },

  unblockUser: async (targetUserId: number): Promise<GlobalResponse<UnblockResponseDto>> => {
    return axiosClient.delete<unknown, GlobalResponse<UnblockResponseDto>>(
      `/blocks/${targetUserId}`
    );
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
