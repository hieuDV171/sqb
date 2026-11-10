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
  PracticeCatalogParams,
  PracticeCatalogResponse,
} from '../types/practice.types';

export const practiceService = {
  getPracticeCatalog(params: PracticeCatalogParams) {
    return axiosClient.get<GlobalResponse<PracticeCatalogResponse>, GlobalResponse<PracticeCatalogResponse>>(
      '/sessions/practice-catalog', { params },
    );
  },

  /**
   * 17.2 Lấy danh sách câu hỏi trong phiên để làm bài & tương tác
   * GET /api/v1/sessions/{sessionId}/questions
   */
  getSessionQuestions: async (
    sessionId: number
  ): Promise<GlobalResponse<SessionQuestionsResponse>> => {
    return axiosClient.get<GlobalResponse<SessionQuestionsResponse>, GlobalResponse<SessionQuestionsResponse>>(
      `/sessions/${sessionId}/questions`
    );
  },

  /**
   * 17.3 Trả lời câu hỏi luyện tập (Kiểm tra đúng/sai)
   * POST /api/v1/questions/{questionId}/answer
   */
  answerQuestion: async (
    questionId: number,
    request: AnswerQuestionRequest
  ): Promise<GlobalResponse<AnswerQuestionResponse>> => {
    return axiosClient.post<GlobalResponse<AnswerQuestionResponse>, GlobalResponse<AnswerQuestionResponse>>(
      `/questions/${questionId}/answer`,
      request
    );
  },

  /**
   * 17.4 Lấy thống kê về câu hỏi (Tỷ lệ đúng, độ khó thực tế, phân bố đáp án)
   * GET /api/v1/questions/{questionId}/statistics
   */
  getQuestionStatistics: async (
    questionId: number
  ): Promise<GlobalResponse<QuestionStatisticsResponse>> => {
    return axiosClient.get<GlobalResponse<QuestionStatisticsResponse>, GlobalResponse<QuestionStatisticsResponse>>(
      `/questions/${questionId}/statistics`
    );
  },

  /**
   * 17.5 Đánh giá sao chất lượng câu hỏi
   * POST /api/v1/questions/{questionId}/rate
   */
  rateQuestion: async (
    questionId: number,
    request: RateQuestionRequest
  ): Promise<GlobalResponse<RateQuestionResponse>> => {
    return axiosClient.post<GlobalResponse<RateQuestionResponse>, GlobalResponse<RateQuestionResponse>>(
      `/questions/${questionId}/rate`,
      request
    );
  },

  reportQuestion(questionId: number, description: string) {
    return axiosClient.put<GlobalResponse<void>, GlobalResponse<void>>(`/questions/${questionId}/report`, { description });
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
    return axiosClient.get<GlobalResponse<QuestionRatingsResponse>, GlobalResponse<QuestionRatingsResponse>>(
      `/questions/${questionId}/ratings`,
      {
        params: {
          after,
          limit: limit || 10,
        },
      }
    );
  },
};
