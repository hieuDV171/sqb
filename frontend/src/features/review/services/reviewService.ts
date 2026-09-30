import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  PendingSessionsResponse,
  GetPendingSessionsParams,
  SessionDetailReviewResponse,
  ApproveQuestionsRequest,
  ApproveQuestionsResponse,
  RejectQuestionsRequest,
  EditQuestionRequest,
  EditQuestionResponse,
} from '../types/review.types';

export const reviewService = {
  /**
   * 16.1 Lấy danh sách các phiên nộp đang chờ duyệt
   * GET /api/v1/sessions/pending
   */
  getPendingSessions: async (
    params?: GetPendingSessionsParams
  ): Promise<GlobalResponse<PendingSessionsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<PendingSessionsResponse>>(
      '/sessions/pending',
      {
        params: {
          after: params?.after,
          limit: params?.limit || 10,
          subject_id: params?.subject_id,
          sort_by: params?.sort_by,
        },
      }
    );
    return response.data;
  },

  /**
   * 16.2 Lấy chi tiết phiên nộp để giảng viên duyệt (kèm cảnh báo trùng lặp 3 tầng)
   * GET /api/v1/sessions/{sessionId}
   */
  getSessionDetailForReview: async (
    sessionId: number
  ): Promise<GlobalResponse<SessionDetailReviewResponse>> => {
    const response = await axiosClient.get<GlobalResponse<SessionDetailReviewResponse>>(
      `/sessions/${sessionId}`
    );
    return response.data;
  },

  /**
   * 16.3 Phê duyệt câu hỏi vào ngân hàng chính thức
   * POST /api/v1/sessions/approve
   */
  approveQuestions: async (
    data: ApproveQuestionsRequest
  ): Promise<GlobalResponse<ApproveQuestionsResponse>> => {
    const response = await axiosClient.post<GlobalResponse<ApproveQuestionsResponse>>(
      '/sessions/approve',
      data
    );
    return response.data;
  },

  /**
   * 16.4 Từ chối câu hỏi trong phiên kèm lý do
   * POST /api/v1/sessions/reject
   */
  rejectQuestions: async (
    data: RejectQuestionsRequest
  ): Promise<GlobalResponse<void>> => {
    const response = await axiosClient.post<GlobalResponse<void>>(
      '/sessions/reject',
      data
    );
    return response.data;
  },

  /**
   * 16.5 Giảng viên trực tiếp chỉnh sửa câu hỏi
   * PUT /api/v1/questions/{questionId}
   */
  editQuestion: async (
    questionId: number,
    data: EditQuestionRequest
  ): Promise<GlobalResponse<EditQuestionResponse>> => {
    const response = await axiosClient.put<GlobalResponse<EditQuestionResponse>>(
      `/questions/${questionId}`,
      data
    );
    return response.data;
  },

  /**
   * 16.6 Hoàn tất quá trình đánh giá phiên nộp
   * POST /api/v1/sessions/{sessionId}/complete-review
   */
  completeSessionReview: async (
    sessionId: number
  ): Promise<GlobalResponse<void>> => {
    const response = await axiosClient.post<GlobalResponse<void>>(
      `/sessions/${sessionId}/complete-review`
    );
    return response.data;
  },
};
