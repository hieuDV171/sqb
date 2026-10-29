import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  ReviewableSessionsResponse,
  GetReviewableSessionsParams,
  SessionDetailReviewResponse,
  ApproveQuestionsRequest,
  ApproveQuestionsResponse,
  RejectQuestionsRequest,
  EditQuestionRequest,
  EditQuestionResponse,
} from '../types/review.types';

export const reviewService = {
  /**
   * 16.1 Lấy danh sách phiên PENDING/REVIEWING có thể kiểm duyệt
   * GET /api/v1/lecturer/sessions/reviewable
   */
  getReviewableSessions: async (
    params?: GetReviewableSessionsParams
  ): Promise<GlobalResponse<ReviewableSessionsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<ReviewableSessionsResponse>>(
      '/lecturer/sessions/reviewable',
      {
        params: {
          after: params?.after,
          limit: params?.limit || 10,
          course_class_id: params?.course_class_id,
          sort_by: params?.sort_by,
        },
      }
    );
    return response.data;
  },

  /**
   * 16.2 Lấy chi tiết phiên nộp để giảng viên duyệt (kèm cảnh báo trùng lặp 3 tầng)
   * GET /api/v1/lecturer/sessions/{sessionId}
   */
  getSessionDetailForReview: async (
    sessionId: number
  ): Promise<GlobalResponse<SessionDetailReviewResponse>> => {
    const response = await axiosClient.get<GlobalResponse<SessionDetailReviewResponse>>(
      `/lecturer/sessions/${sessionId}`
    );
    return response.data;
  },

  /**
   * 16.3 Phê duyệt câu hỏi vào ngân hàng chính thức
   * POST /api/v1/lecturer/sessions/approve
   */
  approveQuestions: async (
    data: ApproveQuestionsRequest
  ): Promise<GlobalResponse<ApproveQuestionsResponse>> => {
    const response = await axiosClient.post<GlobalResponse<ApproveQuestionsResponse>>(
      '/lecturer/sessions/approve',
      data
    );
    return response.data;
  },

  /**
   * 16.4 Từ chối câu hỏi trong phiên kèm lý do
   * POST /api/v1/lecturer/sessions/reject
   */
  rejectQuestions: async (
    data: RejectQuestionsRequest
  ): Promise<GlobalResponse<void>> => {
    const response = await axiosClient.post<GlobalResponse<void>>(
      '/lecturer/sessions/reject',
      data
    );
    return response.data;
  },

  /**
   * 16.5 Giảng viên trực tiếp chỉnh sửa câu hỏi
   * PUT /api/v1/lecturer/questions/{questionId}
   */
  editQuestion: async (
    questionId: number,
    data: EditQuestionRequest
  ): Promise<GlobalResponse<EditQuestionResponse>> => {
    const response = await axiosClient.put<GlobalResponse<EditQuestionResponse>>(
      `/lecturer/questions/${questionId}`,
      data
    );
    return response.data;
  },

  /**
   * 16.6 Hoàn tất quá trình đánh giá phiên nộp
   * POST /api/v1/lecturer/sessions/{sessionId}/complete-review
   */
  completeSessionReview: async (
    sessionId: number
  ): Promise<GlobalResponse<void>> => {
    const response = await axiosClient.post<GlobalResponse<void>>(
      `/lecturer/sessions/${sessionId}/complete-review`
    );
    return response.data;
  },
};
