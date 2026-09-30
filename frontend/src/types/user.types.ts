import type { UserRole } from '@/stores/useAuthStore';
import type { CursorResponse as BaseCursorResponse } from '@/types/response.types';

export interface CursorPaginationDto {
  before?: number | null;
  after?: number | null;
  hasPrev?: boolean;
  hasNext?: boolean;
}

export type CursorResponse<T> = BaseCursorResponse<T>;

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export type FriendshipStatus = 'NONE' | 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface Relationship {
  isFriend: boolean;
  isFollowing: boolean;
  isFollowed: boolean;
  friendRequestStatus: FriendshipStatus;
}

export interface ProfileResponse {
  userId: number;
  email: string;
  fullName: string;
  avatarUrl?: string | null;
  coverUrl?: string | null;
  frameUrl?: string | null;
  gender?: Gender | null;
  dateOfBirth?: string | null;
  bio?: string | null;
  schoolFaculty?: string | null;
  major?: string | null;
  className?: string | null;
  studentLecturerCode?: string | null;
  role: UserRole;
  timezone?: string | null;
  profileCompleted: boolean;
  verified: boolean;

  // Counters & Gamification
  totalProposedQuestions: number;
  gamificationPoints: number;
  badgesCount: number;
  friendsCount: number;
  followersCount: number;
  followingCount: number;

  createdAt?: string | null;
  relationships?: Relationship | null;
}

export interface UpdateProfileRequest {
  fullName?: string;
  avatarUrl?: string;
  coverUrl?: string;
  bio?: string;
  timezone?: string;
  gender?: Gender;
  dateOfBirth?: string; // YYYY-MM-DD
}

export interface AuthorDto {
  userId?: number | null;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
}

// Friendship DTOs
export interface SendFriendRequestDto {
  addresseeId: number;
}

export interface AcceptFriendRequestDto {
  requesterId: number;
}

export interface DeclineFriendRequestDto {
  requesterId: number;
}

export interface FriendshipResponseDto {
  requesterId: number;
  addresseeId: number;
  status: FriendshipStatus;
  createdAt: string;
  acceptedAt?: string | null;
}

export interface FriendDto {
  userId: number;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
  friendsSince: string;
  mutualFriendsCount?: number;
}

export interface FriendListResponseDto {
  items: FriendDto[];
  pagination: CursorPaginationDto;
  totalFriends: number;
}

export interface FriendRequestReceivedDto {
  requester: AuthorDto;
  status: FriendshipStatus;
  createdAt: string;
  mutualFriendsCount?: number;
}

export interface FriendRequestReceivedListResponseDto {
  items: FriendRequestReceivedDto[];
  pagination: CursorPaginationDto;
  totalPending: number;
}

export interface FriendRequestSentDto {
  receiver: AuthorDto;
  status: FriendshipStatus;
  createdAt: string;
  mutualFriendsCount?: number;
}

export interface FriendRequestSentListResponseDto {
  items: FriendRequestSentDto[];
  pagination: CursorPaginationDto;
  totalSent: number;
}

// Follow DTOs
export interface FollowResponseDto {
  targetUserId: number;
  isFollowing: boolean;
  newFollowersCount: number;
  followedAt: string;
}

export interface UnfollowResponseDto {
  targetUserId: number;
  isFollowing: boolean;
  newFollowersCount: number;
}

export interface FollowingUserDto {
  userId: number;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
  role: UserRole;
  schoolFaculty?: string | null;
  code?: string | null;
  followedAt: string;
  isFollowingMe: boolean;
  isFriend: boolean;
}

export interface FollowingListResponseDto {
  items: FollowingUserDto[];
  pagination: CursorPaginationDto;
  totalFollowing: number;
}

export interface FollowerUserDto {
  userId: number;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
  role: UserRole;
  schoolFaculty?: string | null;
  code?: string | null;
  followedMeAt: string;
  isFollowingBack: boolean;
  isFriend: boolean;
}

export interface FollowerListResponseDto {
  items: FollowerUserDto[];
  pagination: CursorPaginationDto;
  totalFollowers: number;
}

export interface RelationshipStatsResponseDto {
  userId: number;
  counts: {
    followersCount: number;
    followingCount: number;
    friendsCount: number;
  };
  relationshipWithMe?: {
    isFollowing: boolean;
    isFollowedBy: boolean;
    isFriend: boolean;
    friendRequestStatus: FriendshipStatus;
    isBlocked: boolean;
  };
}

// Block DTOs
export interface BlockResponseDto {
  targetUserId: number;
  blockedAt: string;
}

export interface UnblockResponseDto {
  targetUserId: number;
}

export interface BlockedUserDto {
  userId: number;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
  blockedAt: string;
}

export interface BlockedListResponseDto {
  items: BlockedUserDto[];
  pagination: CursorPaginationDto;
  totalBlocked: number;
}
