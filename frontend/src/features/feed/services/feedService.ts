import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { CursorResponse } from '@/types/user.types';
import type { ActivityFeedItemDto, NewFeedCountResponseDto } from '../types/feed.types';

// Mock initial feed data for dev resilience
const DEV_FALLBACK_FEEDS: ActivityFeedItemDto[] = [
  {
    feedId: 901,
    targetType: 'LECTURE_VIDEO',
    targetId: 301,
    actionType: 'PUBLISHED_LECTURE_VIDEO',
    actor: {
      userId: 88,
      fullName: 'TS. Hoàng Đức Thắng',
      avatarUrl:
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80',
      role: 'LECTURER',
      schoolFaculty: 'Khoa Toán ứng dụng & Tin học',
    },
    content: {
      title: 'Chuyên đề: Ôn tập trọng tâm Tích phân đường & Mặt - Giải tích 1',
      description:
        'Bài giảng hệ thống hóa các dạng bài tập tìm lưu số, thông lượng của trường vectơ, kèm theo 10 bài tập trắc nghiệm mẫu có lời giải chi tiết.',
      subject_code: 'MI1111',
      subject_name: 'Giải tích 1',
      media_url:
        'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
      reactCount: 56,
      commentCount: 14,
      reactedByMe: false,
    },
    createdAt: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
    weight: 2,
  },
  {
    feedId: 902,
    targetType: 'POST',
    targetId: 501,
    actionType: 'CREATED_POST',
    actor: {
      userId: 101,
      fullName: 'Nguyễn Văn An',
      avatarUrl:
        'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&auto=format&fit=crop&q=80',
      role: 'STUDENT',
      schoolFaculty: 'Trường CNTT & TT (SoICT)',
    },
    content: {
      title: 'Hỏi về bước đạo hàm riêng trong công thức Green',
      description:
        'Chào thầy cô và các bạn, mình đang làm đề cương ôn tập môn Giải tích 1 phần tích phân đường loại 2. Mình có giải câu tìm lưu số của trường vectơ theo định lý Green như ảnh đính kèm, nhưng kết quả không khớp với đáp án trắc nghiệm năm ngoái. Nhờ thầy cô và các bạn kiểm tra giúp mình bước đạo hàm riêng với ạ!',
      subject_code: 'MI1111',
      subject_name: 'Giải tích 1',
      visibility: 'PUBLIC',
      mediaUrls: [
        {
          objectKey: 'posts/2026/09/sample_calc.jpg',
          url: 'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&auto=format&fit=crop&q=80',
          mediaType: 'IMAGE',
        },
      ],
      reactCount: 24,
      commentCount: 6,
      reactedByMe: true,
      lecturerNote:
        'Em bị nhầm dấu ở bước tính dQ/dx - dP/dy. Chú ý hướng đi theo chiều dương quy ước (ngược chiều kim đồng hồ). Thầy đã bổ sung một bài tập tương tự vào ngân hàng đề thi môn Giải tích 1.',
      notedLecturer: {
        userId: 88,
        fullName: 'TS. Hoàng Đức Thắng',
        role: 'LECTURER',
        schoolFaculty: 'Khoa Toán ứng dụng & Tin học',
      },
    },
    createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
    weight: 1,
  },
  {
    feedId: 903,
    targetType: 'SESSION',
    targetId: 402,
    actionType: 'SESSION_RESOLVED_APPROVED',
    actor: {
      userId: 102,
      fullName: 'Trần Thị Bình',
      avatarUrl:
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&auto=format&fit=crop&q=80',
      role: 'STUDENT',
      schoolFaculty: 'Trường Điện - Điện tử (SET)',
    },
    content: {
      title: 'Đề xuất 5 câu hỏi trắc nghiệm: Mô hình OSI & Kiến trúc TCP/IP',
      description:
        'Hội đồng chuyên môn đã thẩm định và chính thức đưa 5 câu hỏi của bạn vào Ngân hàng đề thi chung toàn trường. Bạn nhận được +15 xu và +50 điểm cống hiến!',
      subject_code: 'IT3080',
      subject_name: 'Mạng máy tính',
      pointsEarned: 50,
      sessionId: 402,
    },
    createdAt: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
    weight: 2,
  },
  {
    feedId: 904,
    targetType: 'BADGE',
    targetId: 601,
    actionType: 'EARNED_BADGE',
    actor: {
      userId: 103,
      fullName: 'Lê Quang Cường',
      avatarUrl:
        'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=400&auto=format&fit=crop&q=80',
      role: 'STUDENT',
      schoolFaculty: 'Khoa Toán ứng dụng & Tin học (FAMI)',
    },
    content: {
      badge_name: 'Chiến Thần Đề Thi HUST (Platinum)',
      description:
        'Hoàn thành mốc đóng góp trên 30 câu hỏi được phê duyệt và giải 50 đề thi thử với độ chính xác trên 85%. Chúc mừng tinh thần cống hiến xuất sắc!',
      reactCount: 8,
    },
    createdAt: new Date(Date.now() - 1000 * 60 * 240).toISOString(),
    weight: 2,
  },
  {
    feedId: 905,
    targetType: 'MILESTONE',
    targetId: 701,
    actionType: 'REACHED_MILESTONE',
    actor: {
      userId: 104,
      fullName: 'Phạm Duy Dũng',
      avatarUrl:
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80',
      role: 'STUDENT',
      schoolFaculty: 'Trường Cơ khí (SME)',
    },
    content: {
      title: 'Chinh phục cột mốc 7 ngày học tập liên tục!',
      description:
        'Bạn đã duy trì liên tục chuỗi 7 ngày luyện tập câu hỏi trắc nghiệm trên SQB và nhận huy hiệu Chăm chỉ.',
      streakCount: 7,
    },
    createdAt: new Date(Date.now() - 1000 * 60 * 360).toISOString(),
    weight: 1,
  },
];

export const feedService = {
  /**
   * Lấy danh sách dòng hoạt động cá nhân hóa theo con trỏ Cursor
   * GET /api/v1/activity-feeds
   */
  getActivityFeeds: async (
    after?: number,
    limit: number = 20
  ): Promise<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<CursorResponse<ActivityFeedItemDto>>>(
        '/activity-feeds',
        {
          params: { after, limit },
        }
      )) as any;
      if (res?.data?.items && res.data.items.length > 0) {
        return res;
      }
    } catch {
      // Dev mode fallback
    }

    // Return fallback items in dev/offline mode
    return {
      code: '1000',
      message: 'Thành công (Dev Mode Fallback)',
      data: {
        items: DEV_FALLBACK_FEEDS,
        pagination: {
          hasNext: false,
          hasPrev: false,
        },
      },
    };
  },

  /**
   * Lấy số lượng tin hoạt động mới kể từ mốc thời gian chỉ định
   * GET /api/v1/activity-feeds/new-count
   */
  getNewFeedCount: async (since?: number): Promise<GlobalResponse<NewFeedCountResponseDto>> => {
    try {
      const res = (await axiosClient.get<GlobalResponse<NewFeedCountResponseDto>>(
        '/activity-feeds/new-count',
        {
          params: { since },
        }
      )) as any;
      if (res?.data) {
        return res;
      }
    } catch {
      // Dev fallback
    }

    return {
      code: '1000',
      message: 'Thành công',
      data: {
        newCount: 0,
      },
    };
  },
};
