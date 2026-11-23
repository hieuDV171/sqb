import { axiosClient } from '@/api/axiosClient';
import humps from 'humps';
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
  AiRefinement,
  AiRefinementEvent,
  AiRefinementPreview,
} from '../types/review.types';

export const reviewService = {
  refineQuestion: async (questionId: number, prompt: string, previousEditLogId: number | undefined,
    onPreview: (preview: AiRefinementPreview) => void): Promise<AiRefinement> => {
    let offset = 0;
    let result: AiRefinement | undefined;
    let failure: Error | undefined;

    function readEvents(text: string) {
      if (failure) return;
      let end = text.indexOf('\n\n', offset);
      while (end !== -1) {
        const frame = text.slice(offset, end);
        offset = end + 2;
        if (frame.startsWith('data:')) {
          try {
            const event = humps.camelizeKeys(JSON.parse(frame.slice(5))) as AiRefinementEvent;
            switch (event.type) {
              case 'proposal': onPreview(event.data); break;
              case 'complete': result = event.data; break;
              case 'error': failure = new Error(event.data.message); return;
            }
          } catch {
            failure = new Error('Không đọc được phản hồi tinh chỉnh. Hãy gửi lại yêu cầu.');
            return;
          }
        }
        end = text.indexOf('\n\n', offset);
      }
    }

    const response = await axiosClient.post<unknown, string>(`/questions/${questionId}/ai-refine/stream`,
      { prompt, previousEditLogId }, {
        adapter: 'xhr',
        headers: { Accept: 'text/event-stream, application/json' },
        skipToast: true,
        timeout: 85_000,
        onDownloadProgress: ({ event }) => {
          const request: unknown = event?.target;
          if (request instanceof XMLHttpRequest && request.status === 200) readEvents(request.responseText);
        },
      });
    if (typeof response === 'string') readEvents(response);
    if (failure) throw failure;
    if (!result) throw new Error('Lượt tinh chỉnh bị gián đoạn. Hãy gửi lại yêu cầu.');
    return result;
  },

  applyRefinement: (questionId: number, editLogId: number, approve: boolean, auditAcknowledged: boolean) =>
    axiosClient.post<unknown, GlobalResponse<unknown>>(`/questions/${questionId}/ai-apply`,
      { editLogId, approve, auditAcknowledged }, { skipToast: true }),
  /**
   * 16.1 Lấy danh sách phiên PENDING/REVIEWING có thể kiểm duyệt
   * GET /api/v1/lecturer/sessions/reviewable
   */
  getReviewableSessions: async (
    params?: GetReviewableSessionsParams
  ): Promise<GlobalResponse<ReviewableSessionsResponse>> => {
    return (await axiosClient.get<GlobalResponse<ReviewableSessionsResponse>>(
      '/lecturer/sessions/reviewable',
      {
        params: {
          after: params?.after,
          before: params?.before,
          q: params?.q,
          limit: params?.limit || 10,
          course_class_id: params?.course_class_id,
        },
      }
    )) as unknown as GlobalResponse<ReviewableSessionsResponse>;
  },

  /**
   * 16.2 Lấy chi tiết phiên nộp để giảng viên duyệt (kèm cảnh báo trùng lặp 3 tầng)
   * GET /api/v1/lecturer/sessions/{sessionId}
   */
  getSessionDetailForReview: async (
    sessionId: number
  ): Promise<GlobalResponse<SessionDetailReviewResponse>> => {
    return (await axiosClient.get<GlobalResponse<SessionDetailReviewResponse>>(
      `/lecturer/sessions/${sessionId}`
    )) as unknown as GlobalResponse<SessionDetailReviewResponse>;
  },

  /**
   * 16.3 Phê duyệt câu hỏi vào ngân hàng chính thức
   * POST /api/v1/lecturer/sessions/approve
   */
  approveQuestions: async (
    data: ApproveQuestionsRequest
  ): Promise<GlobalResponse<ApproveQuestionsResponse>> => {
    return (await axiosClient.post<GlobalResponse<ApproveQuestionsResponse>>(
      '/lecturer/sessions/approve',
      data,
      { skipToast: true },
    )) as unknown as GlobalResponse<ApproveQuestionsResponse>;
  },

  /**
   * 16.4 Từ chối câu hỏi trong phiên kèm lý do
   * POST /api/v1/lecturer/sessions/reject
   */
  rejectQuestions: async (
    data: RejectQuestionsRequest
  ): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      '/lecturer/sessions/reject',
      data,
      { skipToast: true },
    )) as unknown as GlobalResponse<void>;
  },

  /**
   * 16.5 Giảng viên trực tiếp chỉnh sửa câu hỏi
   * PUT /api/v1/lecturer/questions/{questionId}
   */
  retryDuplicateCheck: (questionId: number) =>
    axiosClient.post(`/lecturer/questions/${questionId}/retry-duplicate-check`),

  editQuestion: async (
    questionId: number,
    data: EditQuestionRequest
  ): Promise<GlobalResponse<EditQuestionResponse>> => {
    return (await axiosClient.put<GlobalResponse<EditQuestionResponse>>(
      `/lecturer/questions/${questionId}`,
      data,
      { skipToast: true },
    )) as unknown as GlobalResponse<EditQuestionResponse>;
  },

  /**
   * 16.6 Hoàn tất quá trình đánh giá phiên nộp
   * POST /api/v1/lecturer/sessions/{sessionId}/complete-review
   */
  completeSessionReview: async (
    sessionId: number
  ): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      `/lecturer/sessions/${sessionId}/complete-review`,
      undefined,
      { skipToast: true },
    )) as unknown as GlobalResponse<void>;
  },
};
