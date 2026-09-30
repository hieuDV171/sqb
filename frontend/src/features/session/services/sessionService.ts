import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  ProposeSessionRequest,
  ProposeSessionResponse,
  SubjectResponse,
  SubmissionsResponse,
  MySubmissionDetailResponse,
  UpdateSubmissionSessionRequest,
  GetMySubmissionsParams,
  Game2PredictionRequest,
  GamePredictionResponse,
} from '../types/session.types';

export const sessionService = {
  /**
   * 15.1 Đề xuất phiên nộp câu hỏi mới
   * POST /api/v1/sessions/propose
   */
  proposeSession: async (
    data: ProposeSessionRequest
  ): Promise<GlobalResponse<ProposeSessionResponse>> => {
    const response = await axiosClient.post<GlobalResponse<ProposeSessionResponse>>(
      '/sessions/propose',
      data
    );
    return response.data;
  },

  /**
   * 15.2 Lấy danh sách môn học sinh viên đang tham gia
   * GET /api/v1/sessions/my-enrolled-subjects
   */
  getMyEnrolledSubjects: async (): Promise<GlobalResponse<SubjectResponse[]>> => {
    const response = await axiosClient.get<GlobalResponse<SubjectResponse[]>>(
      '/sessions/my-enrolled-subjects'
    );
    return response.data;
  },

  /**
   * 15.3 Lấy danh sách các phiên nộp câu hỏi của tôi
   * GET /api/v1/sessions/my-submissions
   * Query params: after, limit, subject_id, status
   */
  getMySubmissions: async (
    params?: GetMySubmissionsParams
  ): Promise<GlobalResponse<SubmissionsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<SubmissionsResponse>>(
      '/sessions/my-submissions',
      {
        params: {
          after: params?.after,
          limit: params?.limit || 10,
          subject_id: params?.subject_id,
          status: params?.status,
        },
      }
    );
    return response.data;
  },

  /**
   * 15.4 Lấy chi tiết phiên nộp câu hỏi của tôi
   * GET /api/v1/sessions/my-submissions/{sessionId}
   */
  getMySubmissionDetail: async (
    sessionId: number
  ): Promise<GlobalResponse<MySubmissionDetailResponse>> => {
    const response = await axiosClient.get<GlobalResponse<MySubmissionDetailResponse>>(
      `/sessions/my-submissions/${sessionId}`
    );
    return response.data;
  },

  /**
   * 15.5 Cập nhật phiên nộp câu hỏi (khi ở trạng thái PENDING)
   * PUT /api/v1/sessions/my-submissions/{sessionId}
   */
  updateSubmission: async (
    sessionId: number,
    data: UpdateSubmissionSessionRequest
  ): Promise<GlobalResponse<void>> => {
    const response = await axiosClient.put<GlobalResponse<void>>(
      `/sessions/my-submissions/${sessionId}`,
      data
    );
    return response.data;
  },

  /**
   * 15.6 Xóa phiên nộp câu hỏi (khi ở trạng thái PENDING)
   * DELETE /api/v1/sessions/my-submissions/{sessionId}
   */
  deleteSubmission: async (
    sessionId: number
  ): Promise<GlobalResponse<void>> => {
    const response = await axiosClient.delete<GlobalResponse<void>>(
      `/sessions/my-submissions/${sessionId}`
    );
    return response.data;
  },

  /**
   * Đặt cược Game 2: Dự đoán số câu hỏi do LLM / Tự biên soạn được duyệt
   * POST /api/v1/games/prediction/approved-questions
   */
  predictGame2: async (
    data: Game2PredictionRequest
  ): Promise<GlobalResponse<GamePredictionResponse>> => {
    const response = await axiosClient.post<GlobalResponse<GamePredictionResponse>>(
      '/games/prediction/approved-questions',
      data
    );
    return response.data;
  },
};
