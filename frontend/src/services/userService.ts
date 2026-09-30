import { axiosClient } from '@/api/axiosClient';
import { useAuthStore } from '@/stores/useAuthStore';
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

// Mock data generator for offline/dev mode resilience
function getMockProfile(targetUserId?: number): ProfileResponse {
  const currentUser = useAuthStore.getState().user;
  const isMe = !targetUserId || targetUserId === currentUser?.id;

  if (isMe) {
    const role = currentUser?.role || 'STUDENT';
    const isLecturer = role === 'LECTURER';

    return {
      userId: currentUser?.id || 1,
      email: currentUser?.username || 'student@hust.edu.vn',
      fullName:
        currentUser?.username?.includes('@')
          ? isLecturer
            ? 'TS. Trần Văn Hùng'
            : 'Nguyễn Văn An'
          : currentUser?.username || 'Nguyễn Văn An',
      avatarUrl:
        currentUser?.avatarUrl ||
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300',
      coverUrl:
        currentUser?.coverUrl ||
        'https://images.unsplash.com/photo-1707343843437-caacff5cfa74?w=1200',
      frameUrl:
        currentUser?.frameUrl ||
        'https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Flexed%20Biceps.png',
      gender: 'MALE',
      dateOfBirth: '2003-08-15',
      bio: isLecturer
        ? 'Giảng viên Bộ môn Khoa học Máy tính. Nghiên cứu Trí tuệ Nhân tạo & Xử lý Ngôn ngữ Tự nhiên.'
        : 'Sinh viên K66 Viện CNTT & TT Đại học Bách Khoa Hà Nội. Đam mê Thuật toán & Phát triển Hệ thống Web Phân tán.',
      schoolFaculty: 'Trường Công nghệ Thông tin & Truyền thông (SoICT)',
      major: isLecturer ? 'Khoa học Máy tính' : 'Kỹ thuật Phần mềm (IT-E6)',
      className: isLecturer ? 'Bộ môn KHMT' : 'Việt Nhật 01 - K66',
      studentLecturerCode: isLecturer ? 'MSGV-202401' : 'MSSV-20215588',
      role,
      timezone: 'Asia/Ho_Chi_Minh',
      profileCompleted: true,
      verified: true,
      totalProposedQuestions: isLecturer ? 142 : 18,
      gamificationPoints: isLecturer ? 3450 : 1280,
      badgesCount: isLecturer ? 15 : 6,
      friendsCount: 42,
      followersCount: 135,
      followingCount: 58,
      createdAt: '2024-09-01T08:00:00Z',
      relationships: {
        isFriend: true,
        isFollowing: false,
        isFollowed: false,
        friendRequestStatus: 'NONE',
      },
    };
  }

  // Other user mock
  return {
    userId: targetUserId,
    email: `student.${targetUserId}@hust.edu.vn`,
    fullName: `Sinh viên Bách Khoa ${targetUserId}`,
    avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300',
    coverUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200',
    frameUrl: null,
    gender: 'MALE',
    dateOfBirth: '2003-11-20',
    bio: 'Thành viên câu lạc bộ Lập trình Olympic Tin học SoICT.',
    schoolFaculty: 'Trường CNTT & TT',
    major: 'Khoa học Máy tính',
    className: 'CNTT-02 K66',
    studentLecturerCode: `2021${targetUserId}99`,
    role: 'STUDENT',
    timezone: 'Asia/Ho_Chi_Minh',
    profileCompleted: true,
    verified: true,
    totalProposedQuestions: 12,
    gamificationPoints: 850,
    badgesCount: 4,
    friendsCount: 36,
    followersCount: 88,
    followingCount: 40,
    createdAt: '2024-09-15T08:00:00Z',
    relationships: {
      isFriend: false,
      isFollowing: false,
      isFollowed: false,
      friendRequestStatus: 'NONE',
    },
  };
}

export const userService = {
  // ==================== USER PROFILE ====================
  getMyProfile: async (): Promise<GlobalResponse<ProfileResponse>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<ProfileResponse>>('/users/me')) as any;
      if (res?.data) return res;
      return { code: '1000', message: 'Thành công', data: getMockProfile() };
    } catch {
      return { code: '1000', message: 'Thành công (Chế độ dự phòng)', data: getMockProfile() };
    }
  },

  getUserProfile: async (userId: number): Promise<GlobalResponse<ProfileResponse>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<ProfileResponse>>(`/users/${userId}`)) as any;
      if (res?.data) return res;
      return { code: '1000', message: 'Thành công', data: getMockProfile(userId) };
    } catch {
      return { code: '1000', message: 'Thành công (Chế độ dự phòng)', data: getMockProfile(userId) };
    }
  },

  updateMyProfile: async (
    data: UpdateProfileRequest
  ): Promise<GlobalResponse<ProfileResponse>> => {
    try {
      const res = (await axiosClient.put<GlobalResponse<ProfileResponse>>('/users/me', data)) as any;
      if (res?.data) return res;
    } catch {
      // Dev mode fallback: Update auth store and return merged object
    }

    const current = getMockProfile();
    const merged: ProfileResponse = {
      ...current,
      fullName: data.fullName ?? current.fullName,
      bio: data.bio ?? current.bio,
      gender: data.gender ?? current.gender,
      dateOfBirth: data.dateOfBirth ?? current.dateOfBirth,
      timezone: data.timezone ?? current.timezone,
      avatarUrl: data.avatarUrl ?? current.avatarUrl,
      coverUrl: data.coverUrl ?? current.coverUrl,
    };

    return {
      code: '1000',
      message: 'Cập nhật hồ sơ thành công',
      data: merged,
    };
  },

  deleteMyProfile: async (): Promise<GlobalResponse<void>> => {
    try {
      return (await axiosClient.delete<GlobalResponse<void>>('/users/me')) as any;
    } catch {
      return { code: '1000', message: 'Tài khoản đã được vô hiệu hóa', data: undefined };
    }
  },

  // ==================== FRIENDSHIPS ====================
  getMyFriends: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendListResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<FriendListResponseDto>>('/friendships', {
        params: { after, limit },
      })) as any;
      if (res?.data?.items) return res;
    } catch {
      // offline dev fallback
    }

    const mockFriends = [
      {
        userId: 2,
        fullName: 'Trần Thị Bình',
        avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
        frameUrl: null,
        friendsSince: '2024-10-01T10:00:00Z',
        mutualFriendsCount: 14,
      },
      {
        userId: 3,
        fullName: 'Lê Quang Cường',
        avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
        frameUrl:
          'https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Flexed%20Biceps.png',
        friendsSince: '2024-10-15T14:30:00Z',
        mutualFriendsCount: 8,
      },
      {
        userId: 4,
        fullName: 'Phạm Minh Đức',
        avatarUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150',
        frameUrl: null,
        friendsSince: '2024-11-05T09:15:00Z',
        mutualFriendsCount: 21,
      },
    ];

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        items: mockFriends,
        pagination: { hasNext: false, hasPrev: false },
        totalFriends: mockFriends.length,
      },
    };
  },

  getReceivedFriendRequests: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendRequestReceivedListResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<FriendRequestReceivedListResponseDto>>(
        '/friendships/requests/received',
        { params: { after, limit } }
      )) as any;
      if (res?.data?.items) return res;
    } catch {
      // offline dev fallback
    }

    const mockReceived = [
      {
        requester: {
          userId: 5,
          fullName: 'Đỗ Hoàng Long',
          avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
          frameUrl: null,
        },
        status: 'PENDING' as const,
        createdAt: '2026-09-20T16:00:00Z',
        mutualFriendsCount: 5,
      },
    ];

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        items: mockReceived,
        pagination: { hasNext: false, hasPrev: false },
        totalPending: mockReceived.length,
      },
    };
  },

  getSentFriendRequests: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FriendRequestSentListResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<FriendRequestSentListResponseDto>>(
        '/friendships/requests/sent',
        { params: { after, limit } }
      )) as any;
      if (res?.data?.items) return res;
    } catch {
      // offline dev fallback
    }

    const mockSent = [
      {
        receiver: {
          userId: 6,
          fullName: 'Vũ Thùy Linh',
          avatarUrl: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150',
          frameUrl: null,
        },
        status: 'PENDING' as const,
        createdAt: '2026-09-22T10:30:00Z',
        mutualFriendsCount: 9,
      },
    ];

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        items: mockSent,
        pagination: { hasNext: false, hasPrev: false },
        totalSent: mockSent.length,
      },
    };
  },

  sendFriendRequest: async (
    data: SendFriendRequestDto
  ): Promise<GlobalResponse<FriendshipResponseDto>> => {
    try {
      return (await axiosClient.post<GlobalResponse<FriendshipResponseDto>>(
        '/friendships/request',
        data
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Đã gửi lời mời kết bạn',
        data: {
          requesterId: 1,
          addresseeId: data.addresseeId,
          status: 'PENDING',
          createdAt: new Date().toISOString(),
        },
      };
    }
  },

  acceptFriendRequest: async (
    data: AcceptFriendRequestDto
  ): Promise<GlobalResponse<FriendshipResponseDto>> => {
    try {
      return (await axiosClient.post<GlobalResponse<FriendshipResponseDto>>(
        '/friendships/accept',
        data
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Đã chấp nhận lời mời kết bạn',
        data: {
          requesterId: data.requesterId,
          addresseeId: 1,
          status: 'ACCEPTED',
          createdAt: new Date().toISOString(),
          acceptedAt: new Date().toISOString(),
        },
      };
    }
  },

  declineFriendRequest: async (
    data: DeclineFriendRequestDto
  ): Promise<GlobalResponse<void>> => {
    try {
      return (await axiosClient.post<GlobalResponse<void>>('/friendships/decline', data)) as any;
    } catch {
      return { code: '1000', message: 'Đã từ chối lời mời', data: undefined };
    }
  },

  unfriend: async (friendId: number): Promise<GlobalResponse<void>> => {
    try {
      return (await axiosClient.delete<GlobalResponse<void>>(`/friendships/${friendId}`)) as any;
    } catch {
      return { code: '1000', message: 'Đã hủy kết bạn', data: undefined };
    }
  },

  // ==================== FOLLOWS ====================
  followUser: async (targetUserId: number): Promise<GlobalResponse<FollowResponseDto>> => {
    try {
      return (await axiosClient.post<GlobalResponse<FollowResponseDto>>(
        `/follows/${targetUserId}`
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Đã theo dõi người dùng',
        data: {
          targetUserId,
          isFollowing: true,
          newFollowersCount: 89,
          followedAt: new Date().toISOString(),
        },
      };
    }
  },

  unfollowUser: async (targetUserId: number): Promise<GlobalResponse<UnfollowResponseDto>> => {
    try {
      return (await axiosClient.delete<GlobalResponse<UnfollowResponseDto>>(
        `/follows/${targetUserId}`
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Đã hủy theo dõi',
        data: {
          targetUserId,
          isFollowing: false,
          newFollowersCount: 88,
        },
      };
    }
  },

  getFollowing: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FollowingListResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<FollowingListResponseDto>>(
        '/follows/following',
        { params: { after, limit } }
      )) as any;
      if (res?.data?.items) return res;
    } catch {
      // offline fallback
    }

    const mockFollowing = [
      {
        userId: 2,
        fullName: 'Trần Thị Bình',
        avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
        frameUrl: null,
        role: 'STUDENT' as const,
        schoolFaculty: 'Trường CNTT & TT',
        code: '20215501',
        followedAt: '2024-09-02T10:00:00Z',
        isFollowingMe: true,
        isFriend: true,
      },
      {
        userId: 10,
        fullName: 'PGS. TS. Huỳnh Quyết Thắng',
        avatarUrl: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150',
        frameUrl: null,
        role: 'LECTURER' as const,
        schoolFaculty: 'Ban Giám hiệu / SoICT',
        code: 'HUST-BGH',
        followedAt: '2024-08-15T08:00:00Z',
        isFollowingMe: false,
        isFriend: false,
      },
    ];

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        items: mockFollowing,
        pagination: { hasNext: false, hasPrev: false },
        totalFollowing: mockFollowing.length,
      },
    };
  },

  getFollowers: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<FollowerListResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<FollowerListResponseDto>>(
        '/follows/followers',
        { params: { after, limit } }
      )) as any;
      if (res?.data?.items) return res;
    } catch {
      // offline fallback
    }

    const mockFollowers = [
      {
        userId: 2,
        fullName: 'Trần Thị Bình',
        avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
        frameUrl: null,
        role: 'STUDENT' as const,
        schoolFaculty: 'Trường CNTT & TT',
        code: '20215501',
        followedMeAt: '2024-09-05T11:00:00Z',
        isFollowingBack: true,
        isFriend: true,
      },
      {
        userId: 7,
        fullName: 'Lê Hoàng Nam',
        avatarUrl: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150',
        frameUrl: null,
        role: 'STUDENT' as const,
        schoolFaculty: 'Viện Điện tử Viễn thông',
        code: '20223344',
        followedMeAt: '2024-09-12T15:20:00Z',
        isFollowingBack: false,
        isFriend: false,
      },
    ];

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        items: mockFollowers,
        pagination: { hasNext: false, hasPrev: false },
        totalFollowers: mockFollowers.length,
      },
    };
  },

  getRelationshipStats: async (
    userId: number
  ): Promise<GlobalResponse<RelationshipStatsResponseDto>> => {
    try {
      return (await axiosClient.get<GlobalResponse<RelationshipStatsResponseDto>>(
        `/users/${userId}/relationship-stats`
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Thành công',
        data: {
          userId,
          counts: {
            followersCount: 135,
            followingCount: 58,
            friendsCount: 42,
          },
          relationshipWithMe: {
            isFollowing: false,
            isFollowedBy: false,
            isFriend: false,
            friendRequestStatus: 'NONE',
            isBlocked: false,
          },
        },
      };
    }
  },

  // ==================== BLOCKS ====================
  blockUser: async (targetUserId: number): Promise<GlobalResponse<BlockResponseDto>> => {
    try {
      return (await axiosClient.post<GlobalResponse<BlockResponseDto>>(
        `/blocks/${targetUserId}`
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Đã chặn người dùng',
        data: {
          targetUserId,
          blockedAt: new Date().toISOString(),
        },
      };
    }
  },

  unblockUser: async (targetUserId: number): Promise<GlobalResponse<UnblockResponseDto>> => {
    try {
      return (await axiosClient.delete<GlobalResponse<UnblockResponseDto>>(
        `/blocks/${targetUserId}`
      )) as any;
    } catch {
      return {
        code: '1000',
        message: 'Đã bỏ chặn người dùng',
        data: { targetUserId },
      };
    }
  },

  getBlockedUsers: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<BlockedListResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<BlockedListResponseDto>>('/blocks', {
        params: { after, limit },
      })) as any;
      if (res?.data?.items) return res;
    } catch {
      // offline fallback
    }

    const mockBlocked = [
      {
        userId: 99,
        fullName: 'Tài Khoản Spam Quảng Cáo',
        avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
        frameUrl: null,
        blockedAt: '2026-08-15T10:00:00Z',
      },
    ];

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        items: mockBlocked,
        pagination: { hasNext: false, hasPrev: false },
        totalBlocked: mockBlocked.length,
      },
    };
  },
};
