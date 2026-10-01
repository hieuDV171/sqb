import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  SessionQuestionsResponse,
  AnswerQuestionRequest,
  AnswerQuestionResponse,
  QuestionStatisticsResponse,
  RateQuestionRequest,
  RateQuestionResponse,
  QuestionRatingsResponse,
  GetUserSessionsParams,
  UserSubmissionsResponse,
} from '../types/practice.types';

export const practiceService = {
  /**
   * 17.1 Lấy danh sách các phiên đề xuất của người dùng
   * GET /api/v1/users/{userId}/sessions
   */
  getUserProposedSessions: async (
    userId: number,
    params?: GetUserSessionsParams
  ): Promise<GlobalResponse<UserSubmissionsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<UserSubmissionsResponse>>(
      `/users/${userId}/sessions`,
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
   * 17.2 Lấy danh sách câu hỏi trong phiên để làm bài & tương tác
   * GET /api/v1/sessions/{sessionId}/questions
   */
  getSessionQuestions: async (
    sessionId: number
  ): Promise<GlobalResponse<SessionQuestionsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<SessionQuestionsResponse>>(
      `/sessions/${sessionId}/questions`
    );
    return response.data;
  },

  /**
   * 17.3 Trả lời câu hỏi luyện tập (Kiểm tra đúng/sai)
   * POST /api/v1/questions/{questionId}/answer
   */
  answerQuestion: async (
    questionId: number,
    request: AnswerQuestionRequest
  ): Promise<GlobalResponse<AnswerQuestionResponse>> => {
    const response = await axiosClient.post<GlobalResponse<AnswerQuestionResponse>>(
      `/questions/${questionId}/answer`,
      request
    );
    return response.data;
  },

  /**
   * 17.4 Lấy thống kê về câu hỏi (Tỷ lệ đúng, độ khó thực tế, phân bố đáp án)
   * GET /api/v1/questions/{questionId}/statistics
   */
  getQuestionStatistics: async (
    questionId: number
  ): Promise<GlobalResponse<QuestionStatisticsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<QuestionStatisticsResponse>>(
      `/questions/${questionId}/statistics`
    );
    return response.data;
  },

  /**
   * 17.5 Đánh giá chất lượng câu hỏi & Báo lỗi (Feedback / Rating / Error Hunter)
   * POST /api/v1/questions/{questionId}/rate
   */
  rateQuestion: async (
    questionId: number,
    request: RateQuestionRequest
  ): Promise<GlobalResponse<RateQuestionResponse>> => {
    const response = await axiosClient.post<GlobalResponse<RateQuestionResponse>>(
      `/questions/${questionId}/rate`,
      request
    );
    return response.data;
  },

  /**
   * 17.6 Lấy danh sách đánh giá của câu hỏi
   * GET /api/v1/questions/{questionId}/ratings
   */
  getQuestionRatings: async (
    questionId: number,
    after?: number,
    limit?: number
  ): Promise<GlobalResponse<QuestionRatingsResponse>> => {
    const response = await axiosClient.get<GlobalResponse<QuestionRatingsResponse>>(
      `/questions/${questionId}/ratings`,
      {
        params: {
          after,
          limit: limit || 10,
        },
      }
    );
    return response.data;
  },
};
